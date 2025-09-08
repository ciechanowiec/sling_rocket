package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AssetsAPITest extends TestEnvironment {

    AssetsAPITest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void basicResource() {
        context.registerInjectActivateService(AssetsAPI.class);
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            Resource basicResource = Optional.ofNullable(resourceResolver.getResource(AssetsAPI.ASSETS_API_PATH))
                .orElseThrow();
            assertAll(
                () -> assertEquals(AssetsAPI.ASSETS_API_RESOURCE_TYPE, basicResource.getResourceType()),
                () -> assertTrue(ResourceUtil.isSyntheticResource(basicResource)),
                () -> assertEquals(AssetsAPI.ASSETS_API_PATH, basicResource.getPath()),
                () -> assertFalse(basicResource.listChildren().hasNext())
            );
        }
    }

    @Test
    void disabledService() {
        context.registerInjectActivateService(AssetsAPI.class, Map.of("is-enabled", false));
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            Resource basicResource = Optional.ofNullable(resourceResolver.getResource(AssetsAPI.ASSETS_API_PATH))
                .orElseThrow();
            assertAll(
                () -> assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, basicResource.getResourceType()),
                () -> assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, basicResource.getPath()),
                () -> assertTrue(ResourceUtil.isNonExistingResource(basicResource)),
                () -> assertFalse(basicResource.listChildren().hasNext())
            );
        }
    }

    @Test
    void extendedPath() {
        context.registerInjectActivateService(AssetsAPI.class);
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            Resource extendedPath = Optional.ofNullable(resourceResolver.getResource(
                AssetsAPI.ASSETS_API_PATH + "/extended"
            )).orElseThrow();
            assertAll(
                () -> assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, extendedPath.getResourceType()),
                () -> assertEquals(Resource.RESOURCE_TYPE_NON_EXISTING, extendedPath.getPath()),
                () -> assertTrue(ResourceUtil.isNonExistingResource(extendedPath)),
                () -> assertFalse(extendedPath.listChildren().hasNext())
            );
        }
    }

    @Test
    void differentPath() {
        context.registerInjectActivateService(AssetsAPI.class);
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            assertNull(resourceResolver.getResource("/very/different/than/api/path"));
        }
    }
}
