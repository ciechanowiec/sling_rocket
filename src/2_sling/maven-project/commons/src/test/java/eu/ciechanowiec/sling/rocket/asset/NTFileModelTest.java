package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NTFileModelTest extends TestEnvironment {

    NTFileModelTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustNotInitOnInvalidType() {
        context.build().resource("/content").commit();
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Resource content = Optional.ofNullable(resourceResolver.getResource("/content")).orElseThrow();
            boolean wasntInitialized = Optional.ofNullable(content.adaptTo(NTFileModel.class)).isEmpty();
            assertTrue(wasntInitialized);
        }
    }
}
