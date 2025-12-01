package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.ref.NotReferenceableException;
import eu.ciechanowiec.sling.rocket.jcr.ref.Referenceable;
import eu.ciechanowiec.sling.rocket.jcr.ref.ReferenceableSimple;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceableSimpleTest extends TestEnvironment {

    ReferenceableSimpleTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustThrowNotReferenceableException() {
        context.build().resource("/content").commit();
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            Resource content = Optional.ofNullable(resourceResolver.getResource("/content")).orElseThrow();
            Referenceable referenceable = new ReferenceableSimple(
                () -> new TargetJCRPath(content), fullResourceAccess
            );
            assertThrows(NotReferenceableException.class, referenceable::jcrUUID);
        }
    }
}
