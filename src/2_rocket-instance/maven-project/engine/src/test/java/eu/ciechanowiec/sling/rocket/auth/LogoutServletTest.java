package eu.ciechanowiec.sling.rocket.auth;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.mockito.Mockito.*;

class LogoutServletTest extends TestEnvironment {

    private LogoutServlet logoutServlet;
    private Authenticator authenticator;

    LogoutServletTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        context.registerInjectActivateService(LogoutAPI.class);
        authenticator = mock(Authenticator.class);
        context.registerService(Authenticator.class, authenticator);
        logoutServlet = context.registerInjectActivateService(LogoutServlet.class);
    }

    @SuppressWarnings("VariableDeclarationUsageDistance")
    @SneakyThrows
    @Test
    void mustLogout() {
        Objects.requireNonNull(context.currentResource(LogoutAPI.LOGOUT_API_PATH));
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        MockSlingJakartaHttpServletResponse response = context.jakartaResponse();
        logoutServlet.doPost(request, response);
        verify(authenticator, times(1)).logout(request, response);
    }
}
