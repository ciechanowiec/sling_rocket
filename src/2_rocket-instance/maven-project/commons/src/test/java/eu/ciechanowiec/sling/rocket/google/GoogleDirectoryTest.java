package eu.ciechanowiec.sling.rocket.google;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GoogleDirectoryTest extends TestEnvironment {

    GoogleDirectoryTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @Test
    void testToString() {
        GoogleDirectory googleDirectory = context.registerInjectActivateService(GoogleDirectory.class);
        assertAll(
            () -> assertTrue(googleDirectory.listUsers().isEmpty()),
            () -> assertTrue(googleDirectory.listGroups().isEmpty()),
            () -> assertTrue(googleDirectory.listGroups("test-group").isEmpty()),
            () -> assertTrue(googleDirectory.retrieveUser("test-user").isEmpty()),
            () -> assertTrue(googleDirectory.retrieveGroup("test-group").isEmpty()),
            () -> assertTrue(googleDirectory.listMembers("test-group").isEmpty())
        );
    }
}
