package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleNodeTest extends TestEnvironment {

    SimpleNodeTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustCreateNodeAndGiveProps() {
        JCRPath nodePathOne = new TargetJCRPath("/content/universal/one");
        JCRPath nodePathTwo = new TargetJCRPath("/content/universal/two");
        SimpleNode simpleNodeOne = new SimpleNode(nodePathOne, fullResourceAccess);
        SimpleNode simpleNodeTwo = new SimpleNode(nodePathTwo, fullResourceAccess);
        simpleNodeOne.ensureNodeExists().ensureNodeExists().nodeProperties().setProperty("namus1", "valus1");
        simpleNodeTwo.nodeProperties().setProperty("namus2", "valus2");
        assertAll(
                () -> assertEquals(
                        "valus1",
                        new SimpleNode(nodePathOne, fullResourceAccess).nodeProperties()
                                .propertyValue("namus1", DefaultProperties.STRING_CLASS)
                                .orElseThrow()
                ),
                () -> assertEquals(
                        "valus2",
                        new SimpleNode(nodePathTwo, fullResourceAccess).nodeProperties()
                                .propertyValue("namus2", DefaultProperties.STRING_CLASS)
                                .orElseThrow()
                )
        );
    }
}
