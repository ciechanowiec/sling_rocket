package eu.ciechanowiec.sling.rocket.google;

import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.Member;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"MultipleStringLiterals", "PMD.LooseCoupling", "PMD.AvoidDuplicateLiterals"})
class GoogleExternalGroupTest {

    @Mock
    private GoogleDirectory googleDirectory;

    private GoogleExternalGroup googleExternalGroup;

    @BeforeEach
    void setup() {
        Group group = new Group();
        group.setEmail("test-group@example.com");
        group.setName("Test Group");
        group.setDescription("Test Group Description");
        group.setId("test-group-id");
        group.setKind("admin#directory#group");
        googleExternalGroup = new GoogleExternalGroup(group, googleDirectory);
    }

    @Test
    void testGetExternalId() {
        ExternalIdentityRef externalId = googleExternalGroup.getExternalId();
        assertEquals("test-group@example.com", externalId.getId());
        assertEquals(GoogleIdentityProvider.class.getSimpleName(), externalId.getProviderName());
    }

    @Test
    void testGetId() {
        String actualID = googleExternalGroup.getId();
        assertEquals("test-group@example.com", actualID);
    }

    @Test
    void testGetPrincipalName() {
        String actualPrincipalName = googleExternalGroup.getPrincipalName();
        assertEquals("test-group@example.com", actualPrincipalName);
    }

    @Test
    void testGetIntermediatePath() {
        String actualIntermediatePath = googleExternalGroup.getIntermediatePath();
        assertEquals(StringUtils.EMPTY, actualIntermediatePath);
    }

    @Test
    void testGetProperties() {
        Map<String, ?> properties = googleExternalGroup.getProperties();
        assertEquals(5, properties.size());
        assertEquals("test-group@example.com", properties.get("email"));
        assertEquals("Test Group", properties.get("name"));
        assertEquals("Test Group Description", properties.get("description"));
        assertEquals("test-group-id", properties.get("id"));
        assertEquals("admin#directory#group", properties.get("kind"));
    }

    @Test
    void testGetDeclaredMembers() {
        Member member1 = new Member().setEmail("member1@example.com");
        Member member2 = new Member().setEmail("member2@example.com");
        List<Member> members = List.of(member1, member2);
        when(googleDirectory.listMembers("test-group@example.com")).thenReturn(members);
        Iterable<ExternalIdentityRef> declaredMembers = googleExternalGroup.getDeclaredMembers();
        int count = 0;
        for (ExternalIdentityRef member : declaredMembers) {
            count++;
            assertTrue(
                member.getId().equals("member1@example.com")
                    || member.getId().equals("member2@example.com")
            );
            assertEquals(GoogleIdentityProvider.class.getSimpleName(), member.getProviderName());
        }
        assertEquals(2, count);
    }

    @SneakyThrows
    @Test
    void testGetDeclaredGroups() {
        Group parentGroup1 = new Group().setEmail("parent1@example.com");
        Group parentGroup2 = new Group().setEmail("parent2@example.com");
        List<Group> groups = List.of(parentGroup1, parentGroup2);
        when(googleDirectory.listGroups("test-group@example.com")).thenReturn(groups);
        Iterable<ExternalIdentityRef> declaredGroups = googleExternalGroup.getDeclaredGroups();
        assertNotNull(declaredGroups);
        int count = 0;
        for (ExternalIdentityRef group : declaredGroups) {
            count++;
            assertTrue(
                group.getId().equals("parent1@example.com")
                    || group.getId().equals("parent2@example.com")
            );
            assertEquals(GoogleIdentityProvider.class.getSimpleName(), group.getProviderName());
        }
        assertEquals(2, count);
    }

    @Test
    void testToString() {
        String toString = googleExternalGroup.toString();
        assertTrue(toString.contains("test-group@example.com"));
        assertTrue(toString.contains("Test Group"));
    }
}
