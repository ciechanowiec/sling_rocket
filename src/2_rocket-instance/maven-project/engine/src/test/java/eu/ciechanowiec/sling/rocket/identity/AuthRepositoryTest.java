package eu.ciechanowiec.sling.rocket.identity;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRepositoryTest extends TestEnvironment {

    AuthRepositoryTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void testAll() {
        createOrGetUser(new AuthIDUser("xx"));
        createOrGetGroup(new AuthIDGroup("group-a"));
        createOrGetGroup(new AuthIDGroup("group-b"));
        createOrGetUser(new AuthIDUser("user-1"));
        createOrGetUser(new AuthIDUser("user-2"));
        createOrGetUser(new AuthIDUser("aa"));
        AuthRepository authRepository = new AuthRepository(fullResourceAccess);
        Set<AuthID> all = authRepository.all();
        assertAll(
            () -> assertEquals(8, all.size()),
            () -> assertEquals(new AuthIDUser("xx"), IteratorUtils.toList(all.iterator()).getLast()),
            () -> assertEquals(new AuthIDUser("aa"), IteratorUtils.toList(all.iterator()).getFirst())
        );
    }
}
