package eu.ciechanowiec.sling.rocket.test;

import eu.ciechanowiec.sling.rocket.identity.AuthIDGroup;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestEnvironmentTest extends TestEnvironment {

    TestEnvironmentTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void duplicatedAuthCreation() {
        AuthIDGroup groupOne = createOrGetGroup(new AuthIDGroup("groupus"));
        AuthIDGroup groupTwo = createOrGetGroup(new AuthIDGroup("groupus"));
        AuthIDUser userOne = createOrGetUser(new AuthIDUser("userus"));
        AuthIDUser userTwo = createOrGetUser(new AuthIDUser("userus"));
        assertAll(
            () -> assertEquals(groupOne, groupTwo),
            () -> assertEquals(userOne, userTwo)
        );
    }
}
