package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BasicReferencableTest extends TestEnvironment {

    BasicReferencableTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustThrowNotReferencableException() {
        context.build().resource("/content").commit();
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Resource content = Optional.ofNullable(resourceResolver.getResource("/content")).orElseThrow();
            String contentPath = content.getPath();
            Referencable referencable = new BasicReferencable(() -> new TargetJCRPath(contentPath), resourceAccess);
            assertThrows(NotReferencableException.class, referencable::jcrUUID);
        }
    }
}
