package eu.ciechanowiec.sling.rocket.jcr;

import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import java.util.Optional;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

class BasicReferencableTest extends TestEnvironment {

    BasicReferencableTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustThrowNotReferencableException() {
        context.build().resource("/content").commit();
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            Resource content = Optional.ofNullable(resourceResolver.getResource("/content")).orElseThrow();
            String contentPath = content.getPath();
            Referencable referencable = new BasicReferencable(() -> new TargetJCRPath(contentPath), fullResourceAccess);
            assertThrows(NotReferencableException.class, referencable::jcrUUID);
        }
    }
}
