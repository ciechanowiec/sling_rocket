package eu.ciechanowiec.sling.rocket.jcr.path;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
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
}
