package eu.ciechanowiec.sling.rocket.google;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GoogleDirectoryTest extends TestEnvironment {

    GoogleDirectoryTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @Test
    void mustNotRegister() {
        assertThrows(
            RuntimeException.class,
            () -> context.registerInjectActivateService(GoogleDirectory.class)
        );
    }
}
