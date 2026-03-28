package eu.ciechanowiec.sling.rocket.observation.audit;

import eu.ciechanowiec.sling.rocket.identity.AuthID;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.identity.creation.AuthCreationBroadcast;
import eu.ciechanowiec.sling.rocket.observation.audit.pushers.AuthenticationEventListener;
import eu.ciechanowiec.sling.rocket.observation.audit.pushers.GenericRCL;
import eu.ciechanowiec.sling.rocket.observation.audit.pushers.SlingPostMonitor;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings(
    {
        "MagicNumber", "MultipleStringLiterals", "PMD.AvoidDuplicateLiterals", "ClassWithTooManyMethods", "MethodCount",
        "PMD.CouplingBetweenObjects", "PMD.TooManyMethods", "PMD.AvoidUsingHardCodedIP"
    }
)
class AuditSystemTest extends TestEnvironment {

    private Storage storage;
    private EntryTrampoline entryTrampoline;
    private JobManager jobManager;

    AuditSystemTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        jobManager = mock(JobManager.class);
        context.registerService(JobManager.class, jobManager);
        storage = context.registerInjectActivateService(Storage.class);
        entryTrampoline = context.registerInjectActivateService(EntryTrampoline.class, Map.of("is-enabled", true));
        context.registerInjectActivateService(
            AuthenticationEventListener.class, Map.of("is-enabled", true)
        );
    }

    @Test
    @SuppressWarnings({"MethodLength", "ExtractMethodRecommender"})
    void testEntryConstructorsAndProperties() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> additionalProps = Map.of("key1", "value1", "key2", "value2");
        Entry entry = new Entry("testUser", "/content/test", now, additionalProps);

        assertAll(
            () -> assertEquals("testUser", entry.userID()),
            () -> assertEquals("/content/test", entry.subject()),
            () -> assertEquals(now, entry.timestamp()),
            () -> assertEquals(2, entry.additionalProperties().size()),
            () -> assertEquals("value1", entry.additionalProperties().get("key1")),
            () -> assertEquals("value2", entry.additionalProperties().get("key2"))
        );

        // Test filtering of system properties
        Map<String, String> propsWithSystem = new ConcurrentHashMap<>(additionalProps);
        propsWithSystem.put(JcrConstants.JCR_CREATED, "filtered");
        propsWithSystem.put(JcrConstants.JCR_CREATED_BY, "testUser2");
        propsWithSystem.put(JcrConstants.JCR_LASTMODIFIED, now.toString());
        propsWithSystem.put(JcrConstants.JCR_LAST_MODIFIED_BY, "testUser2");
        propsWithSystem.put(Entry.PN_USER_ID, "filtered");
        propsWithSystem.put(Entry.PN_SUBJECT, "filtered");
        propsWithSystem.put(Entry.PN_TIMESTAMP, "filtered");
        propsWithSystem.put("irrelevant", "should stay");
        Entry filteredEntry = new Entry("testUser2", "irrelevant", now, propsWithSystem);
        assertAll(
            () -> assertEquals(3, filteredEntry.additionalProperties().size()),
            () -> assertTrue(filteredEntry.additionalProperties().containsKey("key1")),
            () -> assertTrue(filteredEntry.additionalProperties().containsKey("key2")),
            () -> assertTrue(filteredEntry.additionalProperties().containsKey("irrelevant"))
        );

        // Test from ResourceChange
        ResourceChange resourceChange = mock(ResourceChange.class);
        when(resourceChange.getUserId()).thenReturn("changeUser");
        when(resourceChange.getPath()).thenReturn("/content/changed");
        when(resourceChange.getType()).thenReturn(ResourceChange.ChangeType.ADDED);
        Entry entryFromRC = new Entry(resourceChange);
        assertAll(
            () -> assertEquals("changeUser", entryFromRC.userID()),
            () -> assertEquals("/content/changed", entryFromRC.subject()),
            () -> assertNotNull(entryFromRC.timestamp()),
            () -> assertEquals("ADDED", entryFromRC.additionalProperties().get("changeType")),
            () -> assertEquals(ResourceChange.class.getName(), entryFromRC.additionalProperties().get("source"))
        );

        // Test with null userId in ResourceChange
        ResourceChange rcWithoutUser = mock(ResourceChange.class);
        when(rcWithoutUser.getUserId()).thenReturn(null);
        when(rcWithoutUser.getPath()).thenReturn("/content/anon");
        when(rcWithoutUser.getType()).thenReturn(ResourceChange.ChangeType.REMOVED);
        Entry anonymousEntry = new Entry(rcWithoutUser);
        assertAll(
            () -> assertEquals(Entry.UNKNOWN, anonymousEntry.userID()),
            () -> assertEquals("/content/anon", anonymousEntry.subject()),
            () -> assertNotNull(anonymousEntry.timestamp()),
            () -> assertEquals("REMOVED", anonymousEntry.additionalProperties().get("changeType")),
            () -> assertEquals(ResourceChange.class.getName(), anonymousEntry.additionalProperties().get("source"))
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    @SneakyThrows
    void testStorageAndTrampolineIntegration() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 9, 10, 0);
        Entry entryToSave = new Entry("user1", "subject1", timestamp, Map.of("prop1", "val1"));

        // 1. Submit for saving
        entryTrampoline.submitForSaving(entryToSave);

        // 2. Flush by deactivating (triggering flush)
        entryTrampoline.deactivate();

        // 3. Capture job
        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());

        Map<String, Object> jobProps = jobPropsCaptor.getValue();
        Job mockJob = mock(Job.class);
        when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
            .thenReturn((List) jobProps.get(EntriesBatch.PN_ENTRIES_BATCHED));

        // 4. Process job via storage
        JobConsumer.JobResult result = storage.process(mockJob);
        assertEquals(JobConsumer.JobResult.OK, result);

        // 5. Verify storage
        assertTrue(storage.getCount() >= 1);
        List<Entry> entries = storage.entries(2026, 3, 9);
        assertFalse(entries.isEmpty());
        List<Entry> userEntries = entries.stream()
            .filter(entry -> "user1".equals(entry.userID()))
            .toList();
        assertEquals(1, userEntries.size());
        Entry storedEntry = userEntries.getFirst();
        assertEquals("subject1", storedEntry.subject());
        assertEquals("val1", storedEntry.additionalProperties().get("prop1"));
    }

    @Test
    void testStorageDeleteMethods() {
        // Prepare storage with some entries
        Entry entry1 = new Entry("u1", "s1", LocalDateTime.of(2026, 1, 1, 12, 0), Map.of());
        Entry entry2 = new Entry("u2", "s2", LocalDateTime.of(2027, 1, 1, 12, 0), Map.of());

        // Process directly via storage.process
        saveDirectly(List.of(entry1, entry2));

        assertEquals(2, storage.getCount());

        // Delete by year
        storage.delete(2026);
        List<Entry> entries2026 = storage.entries(2026, 1, 1);
        List<Entry> entries2027 = storage.entries(2027, 1, 1);
        assertTrue(entries2026.isEmpty());
        assertFalse(entries2027.isEmpty());

        // Delete all
        storage.deleteAll();
        assertEquals(0, storage.getCount());
    }

    @Test
    void testStorageAsJSON() {
        String json = storage.asJSON();
        assertNotNull(json);
        assertTrue(json.contains("\"numberOfEntries\":0"));
        assertFalse(json.contains("\"name\""));
        assertFalse(json.contains("\"jcrPath\""));
        assertFalse(json.contains("\"storagePath\""));

        // Add an entry and check again
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 9, 10, 0);
        Entry entry = new Entry("user1", "subject1", timestamp, Map.of("prop1", "val1"));
        saveDirectly(List.of(entry));

        String jsonWithEntry = storage.asJSON();
        assertTrue(jsonWithEntry.contains("\"numberOfEntries\":1"));
    }

    private void saveDirectly(List<Entry> entries) {
        Job mockJob = mock(Job.class);
        EntriesBatch batch = new EntriesBatch(entries);
        when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
            .thenReturn(batch.asSlingJobProperties());
        JobConsumer.JobResult jobResult = storage.process(mockJob);
        assertEquals(JobConsumer.JobResult.OK, jobResult);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testAuthenticationEventListenerFlow() {
        // Test login
        Map<String, Object> loginProps = Map.of(
            SlingConstants.PROPERTY_USERID, "loggedUser",
            AuthenticationInfo.AUTH_TYPE, "LDAP"
        );
        EventAdmin eventAdmin = Objects.requireNonNull(context.getService(EventAdmin.class));
        eventAdmin.sendEvent(new Event(AuthConstants.TOPIC_LOGIN, loginProps));

        // Test login failed
        Map<String, Object> failedProps = Map.of(
            SlingConstants.PROPERTY_USERID, "failedUser",
            AuthenticationInfo.AUTH_TYPE, "PWD",
            "reason_code", "INVALID_CREDENTIALS"
        );
        eventAdmin.sendEvent(new Event(AuthConstants.TOPIC_LOGIN_FAILED, failedProps));

        // Test auth creation
        AuthIDUser authID = new AuthIDUser("newUser");
        Map<String, Object> createdProps = Map.of(
            AuthID.class.getSimpleName(), authID
        );
        eventAdmin.sendEvent(new Event(AuthCreationBroadcast.TOPIC_AUTH_CREATION, createdProps));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());

        jobPropsCaptor.getAllValues()
            .forEach(
                props -> {
                    Job mockJob = mock(Job.class);
                    when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                        .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                    assertEquals(JobConsumer.JobResult.OK, storage.process(mockJob));
                }
            );
        assertEquals(3, storage.getCount());
    }

    @SuppressWarnings({"unchecked", "rawtypes", "squid:S2925"})
    @Test
    void testGenericRCLFlow() throws InterruptedException {
        context.registerInjectActivateService(
            GenericRCL.class, Map.of(
                "resource.paths", "/content",
                "resource.change.types", new String[]{"ADDED", "CHANGED", "REMOVED"}
            )
        );
        context.build().resource("/content").commit();
        context.build().resource("/irrelevant").commit();
        TimeUnit.SECONDS.sleep(3);
        entryTrampoline.deactivate();
        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());

        Map<String, Object> props = jobPropsCaptor.getValue();
        Job mockJob = mock(Job.class);
        when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
            .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
        assertEquals(JobConsumer.JobResult.OK, storage.process(mockJob));
        assertEquals(1, storage.getCount());
    }

    @Test
    void testEntryTrampolineDisabled() {
        JobManager mockJobManager = mock(JobManager.class);
        EntryTrampolineConfig disabledConfig = mock(EntryTrampolineConfig.class);
        when(disabledConfig.is$_$enabled()).thenReturn(false);
        EntryTrampoline disabledTrampoline = new EntryTrampoline(mockJobManager, disabledConfig);
        disabledTrampoline.submitForSaving(new Entry("u", "s", LocalDateTime.now(), Map.of()));
        disabledTrampoline.deactivate(); // calls flush
        // Should not have called jobManager.addJob because it's disabled
        verify(mockJobManager, never()).addJob(eq(Storage.JOB_TOPIC), anyMap());
    }

    @Test
    void testJCRPathOfStorage() {
        String jcrPath = storage.getJCRPath();
        assertEquals("/var/audit/eu.ciechanowiec.sling.rocket", jcrPath);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorEnabledSingleModification() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        SlingJakartaHttpServletRequest request = mock(SlingJakartaHttpServletRequest.class);
        when(request.getRemoteUser()).thenReturn("postUser");

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onCreated("/content/node")));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(1, postEntries.size());
        Entry stored = postEntries.getFirst();
        assertAll(
            () -> assertEquals("postUser", stored.userID()),
            () -> assertEquals(Modification.class.getName(), stored.subject()),
            () -> assertEquals("CREATE", stored.additionalProperties().get(ModificationType.class.getName())),
            () -> assertEquals("/content/node", stored.additionalProperties().get("source")),
            () -> assertEquals(Entry.UNKNOWN, stored.additionalProperties().get("destination")),
            () -> assertNotNull(stored.additionalProperties().get("threadName"))
        );
    }

    @Test
    void testSlingPostMonitorDisabledDoesNotSubmitEntries() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", false));

        SlingJakartaHttpServletRequest request = mock(SlingJakartaHttpServletRequest.class);

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onModified("/content/page")));

        entryTrampoline.deactivate();

        verify(jobManager, never()).addJob(eq(Storage.JOB_TOPIC), anyMap());
        assertEquals(0, storage.getCount());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorMultipleModificationsAllStored() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        SlingJakartaHttpServletRequest request = mock(SlingJakartaHttpServletRequest.class);
        when(request.getRemoteUser()).thenReturn("bulkUser");

        List<Modification> modifications = List.of(
            Modification.onCreated("/content/a"),
            Modification.onModified("/content/b"),
            Modification.onDeleted("/content/c")
        );

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, modifications);

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(3, postEntries.size());
        List<String> storedTypes = postEntries.stream()
            .map(entry -> entry.additionalProperties().get(ModificationType.class.getName()))
            .toList();
        assertAll(
            () -> assertTrue(storedTypes.contains("CREATE")),
            () -> assertTrue(storedTypes.contains("MODIFY")),
            () -> assertTrue(storedTypes.contains("DELETE"))
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorNullRemoteUserFallsBackToUnknown() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        SlingJakartaHttpServletRequest request = mock(SlingJakartaHttpServletRequest.class);
        when(request.getRemoteUser()).thenReturn(null);

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onModified("/content/anon")));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(1, postEntries.size());
        assertEquals(Entry.UNKNOWN, postEntries.getFirst().userID());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorMoveRecordsBothSourceAndDestination() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        SlingJakartaHttpServletRequest request = mock(SlingJakartaHttpServletRequest.class);
        when(request.getRemoteUser()).thenReturn("moverUser");

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onMoved("/content/src", "/content/dst")));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(1, postEntries.size());
        Entry stored = postEntries.getFirst();
        assertAll(
            () -> assertEquals("MOVE", stored.additionalProperties().get(ModificationType.class.getName())),
            () -> assertEquals("/content/src", stored.additionalProperties().get("source")),
            () -> assertEquals("/content/dst", stored.additionalProperties().get("destination"))
        );
    }

    @Test
    void testSlingPostMonitorEmptyModificationListDoesNothing() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        SlingJakartaHttpServletRequest request = mock(SlingJakartaHttpServletRequest.class);

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of());

        entryTrampoline.deactivate();

        verify(jobManager, never()).addJob(eq(Storage.JOB_TOPIC), anyMap());
        assertEquals(0, storage.getCount());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorSingleXForwardedForHeader() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setRemoteUser("headerUser");
        request.addHeader(HttpHeader.X_FORWARDED_FOR.name(), "192.168.1.1");

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onCreated("/content/forwarded")));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(1, postEntries.size());
        Entry stored = postEntries.getFirst();
        assertAll(
            () -> assertEquals("headerUser", stored.userID()),
            () -> assertEquals("192.168.1.1", stored.additionalProperties().get(HttpHeader.X_FORWARDED_FOR.name())),
            () -> assertFalse(stored.additionalProperties().containsKey(HttpHeader.X_FORWARDED_FOR.name() + "[1]"))
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorMultipleXForwardedForHeaders() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setRemoteUser("multiHeaderUser");
        request.addHeader(HttpHeader.X_FORWARDED_FOR.name(), "10.0.0.1");
        request.addHeader(HttpHeader.X_FORWARDED_FOR.name(), "10.0.0.2");
        request.addHeader(HttpHeader.X_FORWARDED_FOR.name(), "10.0.0.3");

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onModified("/content/multi")));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(1, postEntries.size());
        Entry stored = postEntries.getFirst();
        assertAll(
            () -> assertEquals("multiHeaderUser", stored.userID()),
            () -> assertEquals(
                "10.0.0.1", stored.additionalProperties().get(HttpHeader.X_FORWARDED_FOR.name() + "[1]")
            ),
            () -> assertEquals(
                "10.0.0.2", stored.additionalProperties().get(HttpHeader.X_FORWARDED_FOR.name() + "[2]")
            ),
            () -> assertEquals(
                "10.0.0.3", stored.additionalProperties().get(HttpHeader.X_FORWARDED_FOR.name() + "[3]")
            ),
            () -> assertFalse(stored.additionalProperties().containsKey(HttpHeader.X_FORWARDED_FOR.name()))
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorSingleXRealIPHeader() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setRemoteUser("realIPUser");
        request.addHeader("X-Real-IP", "203.0.113.42");

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onCreated("/content/realip")));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(1, postEntries.size());
        Entry stored = postEntries.getFirst();
        assertAll(
            () -> assertEquals("realIPUser", stored.userID()),
            () -> assertEquals("203.0.113.42", stored.additionalProperties().get("X-Real-IP")),
            () -> assertFalse(stored.additionalProperties().containsKey("X-Real-IP[1]"))
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorBothForwardedForAndRealIP() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setRemoteUser("dualHeaderUser");
        request.addHeader(HttpHeader.X_FORWARDED_FOR.name(), "172.16.0.1");
        request.addHeader("X-Real-IP", "198.51.100.5");

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onDeleted("/content/dual")));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(1, postEntries.size());
        Entry stored = postEntries.getFirst();
        assertAll(
            () -> assertEquals("dualHeaderUser", stored.userID()),
            () -> assertEquals("172.16.0.1", stored.additionalProperties().get(HttpHeader.X_FORWARDED_FOR.name())),
            () -> assertEquals("198.51.100.5", stored.additionalProperties().get("X-Real-IP"))
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorNoClientHeaders() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setRemoteUser("noHeaderUser");

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onCreated("/content/noheaders")));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(1, postEntries.size());
        Entry stored = postEntries.getFirst();
        assertAll(
            () -> assertEquals("noHeaderUser", stored.userID()),
            () -> assertFalse(stored.additionalProperties().containsKey(HttpHeader.X_FORWARDED_FOR.name())),
            () -> assertFalse(stored.additionalProperties().containsKey("X-Real-IP")),
            () -> assertNotNull(stored.additionalProperties().get("threadName"))
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void testSlingPostMonitorEmptyHeaderEnumeration() {
        context.registerInjectActivateService(SlingPostMonitor.class, Map.of("is-enabled", true));

        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setRemoteUser("emptyHeaderUser");

        SlingPostMonitor monitor = context.getService(SlingPostMonitor.class);
        assertNotNull(monitor);
        monitor.process(request, List.of(Modification.onModified("/content/empty")));

        entryTrampoline.deactivate();

        ArgumentCaptor<Map<String, Object>> jobPropsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jobManager, atLeastOnce()).addJob(eq(Storage.JOB_TOPIC), jobPropsCaptor.capture());
        jobPropsCaptor.getAllValues().forEach(
            props -> {
                Job mockJob = mock(Job.class);
                when(mockJob.getProperty(EntriesBatch.PN_ENTRIES_BATCHED, List.class))
                    .thenReturn((List) props.get(EntriesBatch.PN_ENTRIES_BATCHED));
                storage.process(mockJob);
            }
        );

        List<Entry> postEntries = storage.entries(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth()
            ).stream()
            .filter(entry -> Modification.class.getName().equals(entry.subject()))
            .toList();

        assertEquals(1, postEntries.size());
        Entry stored = postEntries.getFirst();
        assertAll(
            () -> assertEquals("emptyHeaderUser", stored.userID()),
            () -> assertFalse(stored.additionalProperties().containsKey(HttpHeader.X_FORWARDED_FOR.name())),
            () -> assertFalse(stored.additionalProperties().containsKey("X-Real-IP"))
        );
    }
}
