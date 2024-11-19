package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import javax.jcr.Node;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionalPropertyTest extends TestEnvironment {

    ConditionalPropertyTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustThrowNotReferencableException() {
        context.build().resource("/content").commit();
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            Resource content = Optional.ofNullable(resourceResolver.getResource("/content")).orElseThrow();
            Node node = Optional.ofNullable(content.adaptTo(Node.class)).orElseThrow();
            ConditionalProperty conditionalProperty = new ConditionalProperty("non-existent");
            boolean noProperty = conditionalProperty.retrieveFrom(node).isEmpty();
            assertTrue(noProperty);
        }
    }
}
