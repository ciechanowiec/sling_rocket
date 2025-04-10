package eu.ciechanowiec.sling.rocket.jcr.path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import java.util.Optional;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
class TargetJCRPathTest extends TestEnvironment {

    TargetJCRPathTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustThrowForInvalidPath() {
        TargetJCRPath targetJCRPath = new TargetJCRPath("/invalid/");
        assertThrows(InvalidJCRPathException.class, targetJCRPath::get);
    }

    @Test
    void mustCreateOfResource() {
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            SyntheticResource syntheticResource = new SyntheticResource(
                resourceResolver, "/content", "rocket/resource/new"
            );
            context.build()
                .resource("/content")
                .commit();
            Resource resource = Optional.ofNullable(resourceResolver.getResource("/content")).orElseThrow();
            JCRPath contentPath = new TargetJCRPath(resource);
            assertAll(
                () -> assertEquals("/content", contentPath.get()),
                () -> assertThrows(IllegalArgumentException.class, () -> new TargetJCRPath(syntheticResource))
            );
        }
    }

    @SuppressWarnings("EqualsWithItself")
    @Test
    void testEquals() {
        JCRPath targetJCRPath = new TargetJCRPath("/content");
        JCRPath parentJCRPath = new ParentJCRPath(targetJCRPath);
        assertAll(
            () -> assertEquals(targetJCRPath, parentJCRPath),
            () -> assertEquals(targetJCRPath.hashCode(), parentJCRPath.hashCode()),
            () -> assertEquals(targetJCRPath, targetJCRPath),
            () -> assertEquals(parentJCRPath, parentJCRPath),
            () -> assertNotEquals(new Object(), targetJCRPath),
            () -> assertNotEquals(null, targetJCRPath),
            () -> assertNotEquals(new Object(), parentJCRPath),
            () -> assertNotEquals(null, parentJCRPath)
        );
    }
}
