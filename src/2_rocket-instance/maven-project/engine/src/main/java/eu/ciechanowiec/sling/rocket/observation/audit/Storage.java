package eu.ciechanowiec.sling.rocket.observation.audit;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.calendar.*;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import eu.ciechanowiec.sling.rocket.observation.stats.RocketStats;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.jcr.query.Query;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage of {@link Entry}-s.
 */
@Component(
    service = {Storage.class, JobConsumer.class, RocketStats.class},
    immediate = true,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + Storage.JOB_TOPIC,
        "jmx.objectname=eu.ciechanowiec.sling.rocket.engine:type=Audit,name=Storage"
    }
)
@ServiceDescription(Storage.SERVICE_DESCRIPTION)
@Slf4j
@SuppressWarnings("ClassFanOutComplexity")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE
)
public class Storage extends AnnotatedStandardMBean implements JobConsumer, WithJCRPath, StorageMBean, RocketStats {

    static final String JOB_TOPIC = "eu/ciechanowiec/sling/rocket/observation/audit/STORAGE";
    static final String SERVICE_DESCRIPTION = "Storage of audit entries";

    private final TargetJCRPath storagePath;
    private final FullResourceAccess fullResourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                           access to resources
     */
    @SuppressWarnings("MagicNumber")
    @Activate
    public Storage(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess
    ) {
        super(StorageMBean.class);
        this.storagePath = new TargetJCRPath("/var/audit/eu.ciechanowiec.sling.rocket");
        this.fullResourceAccess = fullResourceAccess;
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            Optional.ofNullable(resourceResolver.getResource(storagePath.get()))
                .ifPresentOrElse(
                    resource -> log.info("Storage already exists ({}), skipping initialization", resource),
                    () -> new StagedCalendarNode(Year.of(2025), Year.of(2055), fullResourceAccess).save(
                        storagePath
                    )
                );
        }
    }

    @Override
    @JsonProperty("numberOfEntries")
    public long getCount() {
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            String query = "SELECT * FROM [%s] as node WHERE ISDESCENDANTNODE (node, '%s')".formatted(
                Entry.NT_AUDIT_ENTRY, storagePath.get()
            );
            long count = IteratorUtils.stream(resourceResolver.findResources(query, Query.JCR_SQL2))
                .count();
            log.trace("Counted {} audit entries", count);
            return count;
        }
    }

    @Override
    public String getJCRPath() {
        return jcrPath().get();
    }

    @SuppressWarnings("NestedTryDepth")
    @Override
    public void deleteAll() {
        log.info("Deleting all entries from the storage");
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            CalendarNode storage = new CalendarNode(storagePath, resourceResolver);
            storage.years()
                .stream()
                .map(YearNode::months)
                .flatMap(List::stream)
                .map(MonthNode::days)
                .flatMap(List::stream)
                .map(DayNode::jcrPath)
                .map(JCRPath::get)
                .map(resourceResolver::getResource)
                .filter(Objects::nonNull)
                .map(Resource::getChildren)
                .flatMap(IteratorUtils::stream)
                .forEach(
                    resource -> {
                        try {
                            resourceResolver.delete(resource);
                            resourceResolver.commit();
                        } catch (
                            PersistenceException | UnsupportedOperationException | IllegalStateException exception
                        ) {
                            log.error("Failed to delete {}, skipping deletion", resource, exception);
                        }
                    }
                );
        } finally {
            log.info("Finished deleting all entries from the storage");
        }
    }

    private Optional<Entry> save(Entry entry, ResourceResolver resourceResolver) throws PersistenceException {
        log.trace("Saving {}", entry);
        CalendarNode storage = new CalendarNode(storagePath, resourceResolver);
        LocalDateTime timestamp = entry.timestamp();
        JCRPath dayJCRPath = storage.day(timestamp).orElseThrow(
            () -> new PersistenceException(
                "Failed to obtain day node for timestamp %s, cannot save %s".formatted(timestamp, entry)
            )
        ).jcrPath();
        JCRPath hourJCRPath = new TargetJCRPath(
            new ParentJCRPath(dayJCRPath), String.valueOf(timestamp.getHour())
        );
        JCRPath hourMuniteJCRPath = new TargetJCRPath(
            new ParentJCRPath(hourJCRPath), String.valueOf(timestamp.getMinute())
        );
        JCRPath hourMuniteSecondJCRPath = new TargetJCRPath(
            new ParentJCRPath(hourMuniteJCRPath), "%02d".formatted(timestamp.getSecond())
        );
        JCRPath entryJCRPath = new TargetJCRPath(
            new ParentJCRPath(hourMuniteSecondJCRPath), UUID.randomUUID().toString()
        );
        Resource entryResource = ResourceUtil.getOrCreateResource(
            resourceResolver, entryJCRPath.get(), asJCRProperties(entry),
            JcrResourceConstants.NT_SLING_ORDERED_FOLDER, false
        );
        log.trace("Saved {}", entryResource);
        return Optional.ofNullable(entryResource.adaptTo(Entry.class));
    }

    /**
     * Deletes all entries stored in the {@link Storage} for the specified {@link Year}.
     *
     * @param year {@link Year} for which entries should be deleted
     * @return {@code true} if entries were deleted; {@code false} otherwise
     */
    @SuppressWarnings({"NestedTryDepth", "WeakerAccess"})
    public boolean delete(Year year) {
        log.info("Deleting entries for {}", year);
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            CalendarNode storage = new CalendarNode(storagePath, resourceResolver);
            storage.year(year)
                .map(YearNode::months)
                .orElse(List.of())
                .stream()
                .map(MonthNode::days)
                .flatMap(List::stream)
                .map(DayNode::jcrPath)
                .map(JCRPath::get)
                .map(resourceResolver::getResource)
                .filter(Objects::nonNull)
                .map(Resource::getChildren)
                .flatMap(IteratorUtils::stream)
                .forEach(
                    resource -> {
                        try {
                            resourceResolver.delete(resource);
                        } catch (
                            PersistenceException | UnsupportedOperationException | IllegalStateException exception
                        ) {
                            log.error("Failed to delete entries for {}, skipping deletion", year, exception);
                        }
                    }
                );
            resourceResolver.commit();
            log.info("Finished deleting entries for {}", year);
            return true;
        } catch (PersistenceException exception) {
            log.error("Failed to delete entries for {}", year, exception);
            return false;
        }
    }

    @Override
    public boolean delete(int year) {
        return delete(Year.of(year));
    }

    @Override
    public List<Entry> entries(int year, int month, int day) {
        try {
            LocalDate date = LocalDate.of(year, month, day);
            return entries(date);
        } catch (DateTimeException exception) {
            log.error("Invalid date parameters provided", exception);
            return List.of();
        }
    }

    /**
     * Retrieves all {@link Entry}-s stored in the {@link Storage} for the specified {@link LocalDate}.
     *
     * @param date {@link LocalDate} for which {@link Entry}-s should be retrieved
     * @return all {@link Entry}-s stored in the {@link Storage} for the specified {@link LocalDate}
     */
    @SuppressWarnings("WeakerAccess")
    public List<Entry> entries(LocalDate date) {
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            return entries(date, resourceResolver);
        }
    }

    private List<Entry> entries(LocalDate date, ResourceResolver resourceResolver) {
        CalendarNode storage = new CalendarNode(storagePath, resourceResolver);
        return storage.day(date)
            .map(dayNode -> entries(dayNode, resourceResolver))
            .orElse(List.of());
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private List<Entry> entries(DayNode dayNode, ResourceResolver resourceResolver) {
        JCRPath dayNodeJCRPath = dayNode.jcrPath();
        String query = "SELECT * FROM [%s] as node WHERE ISDESCENDANTNODE (node, '%s')".formatted(
            Entry.NT_AUDIT_ENTRY, dayNodeJCRPath.get()
        );
        return IteratorUtils.stream(resourceResolver.findResources(query, Query.JCR_SQL2))
            .map(resource -> resource.adaptTo(Entry.class))
            .toList();
    }

    private Map<String, Object> asJCRProperties(Entry entry) {
        Map<String, Object> jcrProperties = new ConcurrentHashMap<>();
        jcrProperties.put(Entry.PN_USER_ID, entry.userID());
        jcrProperties.put(Entry.PN_SUBJECT, entry.subject());
        LocalDateTime timestamp = entry.timestamp();
        ZonedDateTime zonedTimestamp = timestamp.atZone(ZoneId.systemDefault());
        jcrProperties.put(Entry.PN_TIMESTAMP, GregorianCalendar.from(zonedTimestamp));
        jcrProperties.put(JcrConstants.JCR_PRIMARYTYPE, Entry.NT_AUDIT_ENTRY);
        jcrProperties.putAll(entry.additionalProperties());
        return Collections.unmodifiableMap(jcrProperties);
    }

    @Override
    @SuppressWarnings("squid:S7467")
    public JobResult process(Job job) {
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            List<Entry> entriesToSave = new EntriesBatch(job).entries();
            List<Entry> entriesSaved = entriesToSave.stream()
                .map(
                    SneakyFunction.sneaky(
                        entry -> save(entry, resourceResolver).orElseThrow(
                            () -> new PersistenceException("Failed to save %s".formatted(entry))
                        )
                    )
                ).toList();
            resourceResolver.commit();
            boolean allEntriesSaved = entriesSaved.size() == entriesToSave.size();
            Conditional.isTrueOrThrow(
                allEntriesSaved, new PersistenceException(
                    "Only %d out of %d entries were saved".formatted(entriesSaved.size(), entriesToSave.size())
                )
            );
            log.debug("Saved {} entry/ies", entriesSaved.size());
            return JobResult.OK;
        } catch (PersistenceException exception) {
            log.error("Failed to process job", exception);
            return JobResult.FAILED;
        }
    }

    @Override
    public JCRPath jcrPath() {
        return storagePath;
    }

    @Override
    public String name() {
        return Storage.class.getName();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
