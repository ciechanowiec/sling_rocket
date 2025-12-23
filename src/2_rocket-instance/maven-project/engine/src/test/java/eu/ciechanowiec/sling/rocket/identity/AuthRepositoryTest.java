package eu.ciechanowiec.sling.rocket.identity;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRepositoryTest extends TestEnvironment {

    AuthRepositoryTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void testAll() {
        createOrGetGroup(new AuthIDGroup("group-a"));
        createOrGetGroup(new AuthIDGroup("group-b"));
        createOrGetUser(new AuthIDUser("user-1"));
        createOrGetUser(new AuthIDUser("user-2"));
        AuthRepository authRepository = new AuthRepository(fullResourceAccess);
        assertEquals(6, authRepository.all().size());
    }
}
