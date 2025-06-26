package eu.ciechanowiec.sling.rocket.google;

import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.UserName;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"MultipleStringLiterals", "PMD.LooseCoupling", "PMD.AvoidDuplicateLiterals"})
class GoogleExternalUserTest {

    @Mock
    private GoogleDirectory googleDirectory;

    private GoogleExternalUser googleExternalUser;
    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setPrimaryEmail("test-user@example.com");
        UserName userName = new UserName();
        userName.setGivenName("Test");
        userName.setFamilyName("User");
        user.setName(userName);
        user.setIsAdmin(true);
        user.setKind("admin#directory#user");
        user.setThumbnailPhotoUrl("http://example.com/photo.jpg");
        googleExternalUser = new GoogleExternalUser(user, googleDirectory);
    }

    @Test
    void testGetExternalId() {
        ExternalIdentityRef externalId = googleExternalUser.getExternalId();
        assertEquals("test-user@example.com", externalId.getId());
        assertEquals(GoogleIdentityProvider.class.getSimpleName(), externalId.getProviderName());
    }

    @Test
    void testGetExternalIdWithNullEmail() {
        user.setPrimaryEmail(null);
        googleExternalUser = new GoogleExternalUser(user, googleDirectory);
        assertThrows(NoSuchElementException.class, googleExternalUser::getExternalId);
    }

    @Test
    void testGetId() {
        String actualID = googleExternalUser.getId();
        assertEquals("test-user@example.com", actualID);
    }

    @Test
    void testGetPrincipalName() {
        String actualPrincipalName = googleExternalUser.getPrincipalName();
        assertEquals("test-user@example.com", actualPrincipalName);
    }

    @Test
    void testGetIntermediatePath() {
        String actualIntermediatePath = googleExternalUser.getIntermediatePath();
        assertEquals("google", actualIntermediatePath);
    }

    @Test
    void testGetDeclaredGroups() {
        Group group1 = new Group().setEmail("group1@example.com");
        Group group2 = new Group().setEmail("group2@example.com");
        List<Group> groups = List.of(group1, group2);
        when(googleDirectory.listGroups("test-user@example.com")).thenReturn(groups);
        Iterable<ExternalIdentityRef> declaredGroups = googleExternalUser.getDeclaredGroups();
        int count = 0;
        for (ExternalIdentityRef group : declaredGroups) {
            count++;
            assertTrue(
                group.getId().equals("group1@example.com")
                    || group.getId().equals("group2@example.com")
            );
            assertEquals(GoogleIdentityProvider.class.getSimpleName(), group.getProviderName());
        }
        assertEquals(2, count);
    }

    @Test
    void testGetDeclaredGroupsWithNullEmail() {
        user.setPrimaryEmail(null);
        googleExternalUser = new GoogleExternalUser(user, googleDirectory);
        Iterable<ExternalIdentityRef> declaredGroups = googleExternalUser.getDeclaredGroups();
        assertFalse(declaredGroups.iterator().hasNext());
        verify(googleDirectory, never()).listGroups(anyString());
    }

    @Test
    void testGetDeclaredGroupsWithEmptyList() {
        when(googleDirectory.listGroups("test-user@example.com")).thenReturn(Collections.emptyList());
        Iterable<ExternalIdentityRef> declaredGroups = googleExternalUser.getDeclaredGroups();
        assertFalse(declaredGroups.iterator().hasNext());
    }

    @Test
    void testGetProperties() {
        Map<String, ?> properties = googleExternalUser.getProperties();
        assertEquals(6, properties.size());
        assertEquals("Test", properties.get("givenName"));
        assertEquals("User", properties.get("familyName"));
        assertEquals("test-user@example.com", properties.get("primaryEmail"));
        assertEquals(true, properties.get("isAdmin"));
        assertEquals("admin#directory#user", properties.get("kind"));
        assertEquals("http://example.com/photo.jpg", properties.get("thumbnailPhotoUrl"));
    }

    @Test
    void testGetPropertiesWithNulls() {
        User userWithNulls = new User();
        googleExternalUser = new GoogleExternalUser(userWithNulls, googleDirectory);
        Map<String, ?> properties = googleExternalUser.getProperties();
        assertEquals(6, properties.size());
        assertEquals("", properties.get("givenName"));
        assertEquals("", properties.get("familyName"));
        assertEquals("", properties.get("primaryEmail"));
        assertEquals(false, properties.get("isAdmin"));
        assertEquals("", properties.get("kind"));
        assertEquals("", properties.get("thumbnailPhotoUrl"));
    }

    @Test
    void testToString() {
        String toString = googleExternalUser.toString();
        assertTrue(toString.startsWith("GoogleExternalUser(user="));
    }
}
