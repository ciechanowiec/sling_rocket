package eu.ciechanowiec.sling.rocket.identity;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SimpleAuthorizableTest extends TestEnvironment {

    SimpleAuthorizableTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void testImpersonation() {
        AuthIDUser userOne = createOrGetUser(new AuthIDUser("userOne"));
        AuthIDUser userTwo = createOrGetUser(new AuthIDUser("userTwo"));
        AuthIDUser userThree = createOrGetUser(new AuthIDUser("userThree"));
        SimpleAuthorizable nonExistentAuth = new SimpleAuthorizable(
                new AuthIDUser(UUID.randomUUID().toString()), fullResourceAccess
        );
        SimpleAuthorizable simpleAuthorizable = new SimpleAuthorizable(userOne, fullResourceAccess);
        assertTrue(simpleAuthorizable.impersonators().isEmpty());
        boolean allWereGranted = simpleAuthorizable.grantImpersonation(
                List.of(
                        userOne, userTwo, userTwo, userThree, new AuthIDUser("non-existent-user")
                )
        );
        assertAll(
                () -> assertFalse(allWereGranted),
                () -> assertFalse(nonExistentAuth.grantImpersonation(List.of(userOne, userTwo))),
                () -> assertFalse(nonExistentAuth.revokeImpersonation(List.of(userOne, userTwo))),
                () -> assertTrue(nonExistentAuth.impersonators().isEmpty()),
                () -> assertEquals(Set.of(userTwo, userThree), simpleAuthorizable.impersonators())
        );
        boolean allWereRevoked = simpleAuthorizable.revokeImpersonation(simpleAuthorizable.impersonators());
        assertAll(
                () -> assertTrue(allWereRevoked),
                () -> assertEquals(Set.of(), simpleAuthorizable.impersonators())
        );
        assertAll(
                () -> assertTrue(simpleAuthorizable.grantImpersonation(List.of(userTwo, userThree))),
                () -> assertEquals(Set.of(userTwo, userThree), simpleAuthorizable.impersonators())
        );
        assertAll(
                () -> assertTrue(simpleAuthorizable.revokeImpersonation(List.of(userThree))),
                () -> assertEquals(Set.of(userTwo), simpleAuthorizable.impersonators())
        );
    }
}
