package eu.ciechanowiec.sling.rocket.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"TypeName", "MultipleStringLiterals", "PMD.AvoidDuplicateLiterals", "PMD.LooseCoupling"})
class GoogleAuthenticationHandlerTest extends TestEnvironment {

    private GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy;
    private GoogleAuthenticationHandler handler;

    GoogleAuthenticationHandlerTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @BeforeEach
    void setup() {
        googleIdTokenVerifierProxy = context.registerInjectActivateService(
            mock(GoogleIdTokenVerifierProxy.class)
        );
        handler = context.registerInjectActivateService(GoogleAuthenticationHandler.class);
    }

    @Test
    @SuppressWarnings("LineLength")
    void testValidToken() {
        MockSlingHttpServletRequest request = context.request();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn("user@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        AuthenticationInfo authenticationInfo = handler.extractCredentials(request, new MockSlingHttpServletResponse());
        assertAll(
            () -> assertTrue(
                authenticationInfo.toString().startsWith(
                    "{user.jcr.credentials=GoogleCredentials(email=user@example.com), sling.authType=GoogleAuth, user.name=user@example.com, user.password"
                )
            ),
            () -> assertEquals("test-id-token", new String(authenticationInfo.getPassword()))
        );
    }

    @Test
    void testInvalidToken() {
        MockSlingHttpServletRequest request = context.request();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.empty());
        AuthenticationInfo authenticationInfo = handler.extractCredentials(request, new MockSlingHttpServletResponse());
        assertAll(
            () -> assertEquals(
                "test-id-token", request.getHeader(GoogleAuthenticationHandler.HEADER_NAME)
            ),
            () -> assertNull(authenticationInfo)
        );
    }

    @Test
    void testNoToken() {
        MockSlingHttpServletRequest request = context.request();
        AuthenticationInfo authenticationInfo = handler.extractCredentials(request, new MockSlingHttpServletResponse());
        assertAll(
            () -> verify(googleIdTokenVerifierProxy, never()).verify(anyString()),
            () -> assertNull(authenticationInfo)
        );
    }

    @Test
    void testDropCredentials() {
        MockSlingHttpServletRequest request = context.request();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        handler.dropCredentials(request, new MockSlingHttpServletResponse());
        assertEquals("test-id-token", request.getHeader(GoogleAuthenticationHandler.HEADER_NAME));
    }

    @Test
    void testRequestCredentials() {
        MockSlingHttpServletRequest request = context.request();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        assertFalse(handler.requestCredentials(request, new MockSlingHttpServletResponse()));
    }
}
