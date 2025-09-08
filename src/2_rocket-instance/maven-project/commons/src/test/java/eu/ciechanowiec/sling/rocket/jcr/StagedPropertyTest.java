package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StagedPropertyTest extends TestEnvironment {

    StagedPropertyTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustSave() {
        context.build().resource("/content/rocket").commit();
        ParentJCRPath targetJCRPath = new ParentJCRPath(new TargetJCRPath("/content/rocket"));
        StagedProperty<String> stagedNode = nodeJCRPath -> {
            NodeProperties nodeProperties = new NodeProperties(nodeJCRPath, fullResourceAccess);
            nodeProperties.setProperty("namus", "valus");
            return nodeProperties.propertyValue("namus", "unknown");
        };
        String actualValue = stagedNode.save(targetJCRPath);
        assertEquals("valus", actualValue);
    }
}
