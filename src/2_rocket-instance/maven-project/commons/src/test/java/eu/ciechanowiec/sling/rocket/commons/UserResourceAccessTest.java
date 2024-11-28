package eu.ciechanowiec.sling.rocket.commons;

import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class UserResourceAccessTest extends TestEnvironment {

    UserResourceAccessTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustBeRRWithUser() {
        AuthIDUser someUser = createOrGetUser(new AuthIDUser("some-user"));
        FullResourceAccess registeredFullResourceAccess = Objects.requireNonNull(
                context.getService(FullResourceAccess.class)
        );
        ResourceAccess userResourceAccess = new UserResourceAccess(someUser, registeredFullResourceAccess);
        String contentPath = "/content";
        context.build().resource(contentPath, Map.of()).commit();
        try (ResourceResolver adminRR = registeredFullResourceAccess.acquireAccess();
                ResourceResolver userRR = userResourceAccess.acquireAccess()) {
            assertAll(
                    () -> assertEquals("admin", adminRR.getUserID()),
                    () -> assertEquals("some-user", userRR.getUserID()),
                    () -> assertNotNull(adminRR.getResource(contentPath)),
                    () -> assertNull(userRR.getResource(contentPath))
            );
        }
    }
}
