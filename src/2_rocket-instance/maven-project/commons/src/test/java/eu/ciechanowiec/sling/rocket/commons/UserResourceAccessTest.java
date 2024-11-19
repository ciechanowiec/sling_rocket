package eu.ciechanowiec.sling.rocket.commons;

import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserResourceAccessTest extends TestEnvironment {

    UserResourceAccessTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void musBeRRWithUser() {
        AuthIDUser someUser = createOrGetUser(new AuthIDUser("some-user"));
        ResourceAccess userResourceAccess = new UserResourceAccess(someUser, fullResourceAccess);
        String contentPath = "/content";
        context.build().resource(contentPath, Map.of()).commit();
        try (ResourceResolver adminRR = fullResourceAccess.acquireAccess();
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
