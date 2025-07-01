package eu.ciechanowiec.sling.rocket.google;

import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.User;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalGroup;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentity;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityRef;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalUser;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings({"TypeName", "PMD.LooseCoupling"})
class GoogleIdentityProviderTest extends TestEnvironment {

    private GoogleDirectory googleDirectory;

    private GoogleIdentityProvider googleIdentityProvider;

    GoogleIdentityProviderTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @BeforeEach
    void setup() {
        googleDirectory = spy(context.registerInjectActivateService(GoogleDirectory.class));
        GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy = context.registerInjectActivateService(
            GoogleIdTokenVerifierProxy.class
        );
        GoogleIdentityProvider googleIdentityProviderTemp = new GoogleIdentityProvider(
            googleDirectory, googleIdTokenVerifierProxy
        );
        googleIdentityProvider = context.registerInjectActivateService(googleIdentityProviderTemp);
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
}
