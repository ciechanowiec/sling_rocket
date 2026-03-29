package eu.ciechanowiec.sling.rocket.identity;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LastLoginRegistryTest extends TestEnvironment {

    private LastLoginRegistry lastLoginRegistry;

    LastLoginRegistryTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @BeforeEach
    void setup() {
        lastLoginRegistry = context.registerInjectActivateService(LastLoginRegistry.class);
    }

    @Test
    void name() {
        assertEquals(LastLoginRegistry.class.getName(), lastLoginRegistry.name());
    }

    @Test
    void postProcessAndAsJSON() {
        AuthenticationInfo info = new AuthenticationInfo(null, "test-user");
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        HttpServletResponse response = new MockSlingJakartaHttpServletResponse();
        lastLoginRegistry.postProcess(info, request, response);
        String json = lastLoginRegistry.asJSON();
        assertAll(
            () -> assertTrue(json.contains("since")),
            () -> assertTrue(json.contains("lastLoginTimes")),
            () -> assertTrue(json.contains("test-user"))
        );
    }

    @Test
    void postProcessNullUser() {
        AuthenticationInfo info = new AuthenticationInfo(null);
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        HttpServletResponse response = new MockSlingJakartaHttpServletResponse();
        lastLoginRegistry.postProcess(info, request, response);
        String json = lastLoginRegistry.asJSON();
        assertFalse(json.contains("test-user"));
    }

    @Test
    void postProcessMultipleUsers() {
        AuthenticationInfo infoUser1 = new AuthenticationInfo(null, "user-1");
        AuthenticationInfo infoUser2 = new AuthenticationInfo(null, "user-2");
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        HttpServletResponse response = new MockSlingJakartaHttpServletResponse();

        lastLoginRegistry.postProcess(infoUser1, request, response);
        lastLoginRegistry.postProcess(infoUser2, request, response);

        String json = lastLoginRegistry.asJSON();
        assertAll(
            () -> assertTrue(json.contains("user-1")),
            () -> assertTrue(json.contains("user-2"))
        );
    }

    @Test
    void postProcessUpdateUser() {
        AuthenticationInfo info = new AuthenticationInfo(null, "test-user");
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        HttpServletResponse response = new MockSlingJakartaHttpServletResponse();

        lastLoginRegistry.postProcess(info, request, response);
        String json1 = lastLoginRegistry.asJSON();
        lastLoginRegistry.postProcess(info, request, response);
        String json2 = lastLoginRegistry.asJSON();

        assertAll(
            () -> assertTrue(json1.contains("test-user")),
            () -> assertTrue(json2.contains("test-user"))
        );
    }
}
