package eu.ciechanowiec.sling.rocket.favicon;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"resource", "PMD.CloseResource"})
class FaviconAPITest extends TestEnvironment {

    FaviconAPITest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        context.registerInjectActivateService(FaviconAPI.class);
    }

    @Test
    void basicResource() {
        ResourceResolver resourceResolver = context.resourceResolver();
        Resource resource = Objects.requireNonNull(resourceResolver.getResource(FaviconAPI.FAVICON_PATH));
        assertAll(
            () -> assertNotNull(resource),
            () -> assertEquals(FaviconAPI.FAVICON_RESOURCE_TYPE, resource.getResourceType()),
            () -> assertFalse(ResourceUtil.isNonExistingResource(resource))
        );
    }

    @Test
    void extendedPath() {
        ResourceResolver resourceResolver = context.resourceResolver();
        String path = FaviconAPI.FAVICON_PATH + "/some/other/path";
        Resource resource = Objects.requireNonNull(resourceResolver.getResource(path));
        assertAll(
            () -> assertNotNull(resource),
            () -> assertTrue(ResourceUtil.isNonExistingResource(resource))
        );
    }

    @Test
    void differentPath() {
        ResourceResolver resourceResolver = context.resourceResolver();
        String path = "/other/path";
        Resource resource = resourceResolver.getResource(path);
        assertNull(resource);
    }
}
