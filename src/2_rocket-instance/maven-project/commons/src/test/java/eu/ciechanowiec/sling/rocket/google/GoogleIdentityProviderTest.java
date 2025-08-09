package eu.ciechanowiec.sling.rocket.google;

import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.User;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalGroup;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentity;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityRef;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalUser;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings(
    {
        "TypeName", "PMD.LooseCoupling", "ClassWithTooManyMethods", "MethodCount", "MultipleStringLiterals",
        "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"
    }
)
class GoogleIdentityProviderTest extends TestEnvironment {

    private GoogleDirectory googleDirectory;

    private GoogleIdentityProvider googleIdentityProvider;

    GoogleIdentityProviderTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @BeforeEach
    void setup() {
        googleDirectory = context.registerInjectActivateService(mock(GoogleDirectory.class));
        context.registerInjectActivateService(GoogleIdTokenVerifierProxy.class);
        googleIdentityProvider = context.registerInjectActivateService(GoogleIdentityProvider.class);
    }

    @Test
    void testGetName() {
        String name = googleIdentityProvider.getName();
        assertEquals(GoogleIdentityProvider.class.getSimpleName(), name);
    }

    @Test
    void testGetIdentityForUser() {
        String userId = "user@example.com";
        User user = new User();
        user.setId("user-id");
        user.setPrimaryEmail(userId);
        when(googleDirectory.retrieveUser(userId)).thenReturn(Optional.of(user));
        String providerName = GoogleIdentityProvider.class.getSimpleName();
        ExternalIdentity identity = googleIdentityProvider.getIdentity(new ExternalIdentityRef(userId, providerName));
        assertAll(
            () -> assertNotNull(identity),
            () -> assertInstanceOf(GoogleExternalUser.class, identity),
            () -> assertEquals(userId, Objects.requireNonNull(identity).getId())
        );
    }

    @Test
    void testGetIdentityForGroup() {
        String groupId = "group@example.com";
        Group group = new Group();
        group.setId("group-id");
        group.setEmail(groupId);
        when(googleDirectory.retrieveUser(groupId)).thenReturn(Optional.empty());
        when(googleDirectory.retrieveGroup(groupId)).thenReturn(Optional.of(group));
        String providerName = GoogleIdentityProvider.class.getSimpleName();
        ExternalIdentity identity = googleIdentityProvider.getIdentity(new ExternalIdentityRef(groupId, providerName));
        assertAll(
            () -> assertNotNull(identity),
            () -> assertInstanceOf(GoogleExternalGroup.class, identity),
            () -> assertEquals(groupId, Objects.requireNonNull(identity).getId())
        );
    }

    @Test
    void testGetIdentityForNotFound() {
        String id = "nonexistent@example.com";
        String providerName = GoogleIdentityProvider.class.getSimpleName();
        when(googleDirectory.retrieveUser(id)).thenReturn(Optional.empty());
        when(googleDirectory.retrieveGroup(id)).thenReturn(Optional.empty());
        ExternalIdentity identity = googleIdentityProvider.getIdentity(new ExternalIdentityRef(id, providerName));
        assertNull(identity);
    }

    @Test
    void testGetIdentityForWrongProvider() {
        String id = "user@example.com";
        String providerName = "WrongProvider";
        ExternalIdentity identity = googleIdentityProvider.getIdentity(new ExternalIdentityRef(id, providerName));
        assertNull(identity);
    }

    @Test
    void testGetUserFound() {
        String userId = "user@example.com";
        User user = new User();
        user.setId("user-id");
        user.setPrimaryEmail(userId);
        when(googleDirectory.retrieveUser(userId)).thenReturn(Optional.of(user));
        ExternalUser externalUser = googleIdentityProvider.getUser(userId);
        assertAll(
            () -> assertNotNull(externalUser),
            () -> assertEquals(userId, Objects.requireNonNull(externalUser).getId())
        );
    }

    @Test
    void testGetUserNotFound() {
        String userId = "nonexistent@example.com";
        when(googleDirectory.retrieveUser(userId)).thenReturn(Optional.empty());
        ExternalUser externalUser = googleIdentityProvider.getUser(userId);
        assertNull(externalUser);
    }

    @Test
    void testGetGroupFound() {
        String groupId = "group@example.com";
        Group group = new Group();
        group.setId("group-id");
        group.setEmail(groupId);
        when(googleDirectory.retrieveGroup(groupId)).thenReturn(Optional.of(group));
        ExternalGroup externalGroup = googleIdentityProvider.getGroup(groupId);
        assertNotNull(externalGroup);
        assertEquals(groupId, externalGroup.getId());
    }

    @Test
    void testGetGroupNotFound() {
        String groupId = "nonexistent@example.com";
        when(googleDirectory.retrieveGroup(groupId)).thenReturn(Optional.empty());
        ExternalGroup externalGroup = googleIdentityProvider.getGroup(groupId);
        assertNull(externalGroup);
    }

    @Test
    void testAuthenticate() {
        Credentials simpleCredentials = new SimpleCredentials("admin", "password".toCharArray());
        Credentials invalidCredentials = new Credentials() {
            // Invalid credentials
        };
        ExternalUser externalUserFromSimpleCreds = googleIdentityProvider.authenticate(simpleCredentials);
        ExternalUser externalUserFromInvalidCreds = googleIdentityProvider.authenticate(invalidCredentials);
        assertAll(
            () -> assertNull(externalUserFromSimpleCreds),
            () -> assertNull(externalUserFromInvalidCreds)
        );
    }

    @Test
    void testListUsers() {
        User user1 = new User();
        user1.setId("user1-id");
        user1.setPrimaryEmail("user1@example.com");
        User user2 = new User();
        user2.setId("user2-id");
        user2.setPrimaryEmail("user2@example.com");
        when(googleDirectory.listUsers()).thenReturn(List.of(user1, user2));
        Iterator<ExternalUser> usersIterator = googleIdentityProvider.listUsers();
        assertNotNull(usersIterator);
        int count = 0;
        while (usersIterator.hasNext()) {
            ExternalUser user = usersIterator.next();
            count++;
            assertTrue(
                user.getId().equals("user1@example.com")
                    || user.getId().equals("user2@example.com")
            );
        }
        assertEquals(2, count);
    }

    @Test
    void testListGroups() {
        Group group1 = new Group();
        group1.setId("group1-id");
        group1.setEmail("group1@example.com");
        Group group2 = new Group();
        group2.setId("group2-id");
        group2.setEmail("group2@example.com");
        when(googleDirectory.listGroups()).thenReturn(List.of(group1, group2));
        Iterator<ExternalGroup> groupsIterator = googleIdentityProvider.listGroups();
        assertNotNull(groupsIterator);
        int count = 0;
        while (groupsIterator.hasNext()) {
            ExternalGroup group = groupsIterator.next();
            count++;
            assertTrue(
                group.getId().equals("group1@example.com")
                    || group.getId().equals("group2@example.com")
            );
        }
        assertEquals(2, count);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testGetCredentialClasses() {
        Set<Class> credentialClasses = googleIdentityProvider.getCredentialClasses();
        assertAll(
            () -> assertEquals(NumberUtils.INTEGER_ONE, credentialClasses.size()),
            () -> assertTrue(credentialClasses.contains(GoogleCredentials.class))
        );
    }

    @Test
    void testGetUserId() {
        Credentials validCredentials = new GoogleCredentials("test-id-token", "test-id-token-signature".toCharArray());
        Credentials invalidCredentials = new GoogleCredentials(null, "test-id-token-signature".toCharArray());
        String validUserId = googleIdentityProvider.getUserId(validCredentials);
        String invalidUserId = googleIdentityProvider.getUserId(invalidCredentials);
        assertAll(
            () -> assertEquals("test-id-token", validUserId),
            () -> assertNull(invalidUserId)
        );
    }

    @Test
    void testGetAttributes() {
        Credentials credentials = new GoogleCredentials("test-id-token", "test-id-token-signature".toCharArray());
        Map<String, ?> attributes = googleIdentityProvider.getAttributes(credentials);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testSetAttributes() {
        Credentials credentials = new GoogleCredentials("test-id-token", "test-id-token-signature".toCharArray());
        assertFalse(googleIdentityProvider.setAttributes(credentials, Map.of("test-key", "test-value")));
    }

    @Test
    void testGetUserFoundCached() {
        User user = new User();
        user.setPrimaryEmail("test@example.com");
        user.setId("12345");
        when(googleDirectory.retrieveUser("12345")).thenReturn(Optional.of(user));
        googleIdentityProvider.getUser("12345");
        googleIdentityProvider.getUser("12345");
        verify(googleDirectory, times(1)).retrieveUser("12345");
    }

    @Test
    void testGetGroupFoundCached() {
        Group group = new Group();
        group.setName("test-group");
        group.setId("group-id");
        when(googleDirectory.retrieveGroup("group-id")).thenReturn(Optional.of(group));
        googleIdentityProvider.getGroup("group-id");
        googleIdentityProvider.getGroup("group-id");
        verify(googleDirectory, times(1)).retrieveGroup("group-id");
    }

    @Test
    void testCacheInvalidation() {
        User user = new User();
        user.setPrimaryEmail("test@example.com");
        user.setId("12345");
        when(googleDirectory.retrieveUser("12345")).thenReturn(Optional.of(user));
        Group group = new Group();
        group.setName("test-group");
        group.setId("group-id");
        when(googleDirectory.retrieveGroup("group-id")).thenReturn(Optional.of(group));
        googleIdentityProvider.getUser("12345");
        googleIdentityProvider.getGroup("group-id");
        googleIdentityProvider.invalidateAllCache();
        googleIdentityProvider.getUser("12345");
        googleIdentityProvider.getGroup("group-id");
        verify(googleDirectory, times(2)).retrieveUser("12345");
        verify(googleDirectory, times(2)).retrieveGroup("group-id");
    }

    @Test
    void testGetUserWithCacheDisabled() {
        User user = new User();
        user.setPrimaryEmail("test@example.com");
        user.setId("12345");
        when(googleDirectory.retrieveUser("12345")).thenReturn(Optional.of(user));
        GoogleIdentityProvider providerWithNoCache = context.registerInjectActivateService(
            GoogleIdentityProvider.class,
            Map.of("cache.ttl.seconds", 0)
        );
        providerWithNoCache.getUser("12345");
        providerWithNoCache.getUser("12345");
        verify(googleDirectory, times(2)).retrieveUser("12345");
    }

    @Test
    void testUserCacheMaxSize() {
        User user1 = new User();
        user1.setPrimaryEmail("user1@example.com");
        user1.setId("user1");
        when(googleDirectory.retrieveUser("user1")).thenReturn(Optional.of(user1));
        User user2 = new User();
        user2.setPrimaryEmail("user2@example.com");
        user2.setId("user2");
        when(googleDirectory.retrieveUser("user2")).thenReturn(Optional.of(user2));
        User user3 = new User();
        user3.setPrimaryEmail("user3@example.com");
        user3.setId("user3");
        when(googleDirectory.retrieveUser("user3")).thenReturn(Optional.of(user3));
        int cacheMaxSize = 1;
        GoogleIdentityProvider providerWithLimitedCache = context.registerInjectActivateService(
            GoogleIdentityProvider.class,
            Map.of("cache.max-size", cacheMaxSize)
        );
        providerWithLimitedCache.getUser("user1");
        providerWithLimitedCache.getUser("user2");
        assertEquals(cacheMaxSize, providerWithLimitedCache.getEstimatedCacheSizeForUsers());
        providerWithLimitedCache.getUser("user3");
        assertEquals(cacheMaxSize, providerWithLimitedCache.getEstimatedCacheSizeForUsers());
        providerWithLimitedCache.getUser("user1");
        verify(googleDirectory, times(2)).retrieveUser("user1");
        verify(googleDirectory, times(1)).retrieveUser("user2");
        verify(googleDirectory, times(1)).retrieveUser("user3");
    }

    @Test
    void testGroupCacheMaxSize() {
        Group group1 = new Group();
        group1.setName("group1");
        group1.setId("group1");
        when(googleDirectory.retrieveGroup("group1")).thenReturn(Optional.of(group1));
        Group group2 = new Group();
        group2.setName("group2");
        group2.setId("group2");
        when(googleDirectory.retrieveGroup("group2")).thenReturn(Optional.of(group2));
        Group group3 = new Group();
        group3.setName("group3");
        group3.setId("group3");
        when(googleDirectory.retrieveGroup("group3")).thenReturn(Optional.of(group3));
        int cacheMaxSize = 1;
        GoogleIdentityProvider providerWithLimitedCache = context.registerInjectActivateService(
            GoogleIdentityProvider.class,
            Map.of("cache.max-size", cacheMaxSize)
        );
        providerWithLimitedCache.getGroup("group1");
        providerWithLimitedCache.getGroup("group2");
        assertEquals(cacheMaxSize, providerWithLimitedCache.getEstimatedCacheSizeForGroups());
        providerWithLimitedCache.getGroup("group3");
        assertEquals(cacheMaxSize, providerWithLimitedCache.getEstimatedCacheSizeForGroups());
        providerWithLimitedCache.getGroup("group1");
        verify(googleDirectory, times(2)).retrieveGroup("group1");
        verify(googleDirectory, times(1)).retrieveGroup("group2");
        verify(googleDirectory, times(1)).retrieveGroup("group3");
    }
}
