package eu.ciechanowiec.sling.rocket.google.auth.sling;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import eu.ciechanowiec.sling.rocket.google.GoogleIdTokenVerifierProxy;
import eu.ciechanowiec.sling.rocket.google.GoogleIdentityProvider;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings(
    {
        "TypeName", "MultipleStringLiterals", "PMD.AvoidDuplicateLiterals", "PMD.LooseCoupling", "PMD.TooManyMethods",
        "ClassWithTooManyMethods", "MethodCount"
    }
)
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
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        AuthenticationInfo authenticationInfo = Objects.requireNonNull(
            handler.extractCredentials(
                request, new MockSlingJakartaHttpServletResponse()
            )
        );
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
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.empty());
        AuthenticationInfo authenticationInfo = handler.extractCredentials(
            request, new MockSlingJakartaHttpServletResponse()
        );
        assertAll(
            () -> assertEquals(
                "test-id-token", request.getHeader(GoogleAuthenticationHandler.HEADER_NAME)
            ),
            () -> assertNull(authenticationInfo)
        );
    }

    @Test
    void testNoToken() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        AuthenticationInfo authenticationInfo = handler.extractCredentials(
            request, new MockSlingJakartaHttpServletResponse()
        );
        assertAll(
            () -> verify(googleIdTokenVerifierProxy, never()).verify(anyString()),
            () -> assertNull(authenticationInfo)
        );
    }

    @Test
    void testRequestCredentials() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        assertFalse(handler.requestCredentials(request, new MockSlingJakartaHttpServletResponse()));
    }

    @Test
    void testValidTokenIsCached() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        handler.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        handler.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        verify(googleIdTokenVerifierProxy, times(1)).verify("test-id-token");
    }

    @Test
    void testInvalidTokenIsCached() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.empty());
        handler.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        handler.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        verify(googleIdTokenVerifierProxy, times(1)).verify("test-id-token");
    }

    @Test
    void testCacheInvalidation() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        handler.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        long numOfCacheEntries = handler.invalidateAllCache();
        handler.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        assertEquals(1, numOfCacheEntries);
        verify(googleIdTokenVerifierProxy, times(2)).verify("test-id-token");
    }

    @Test
    void testGetEstimatedCacheSize() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token-1");
        GoogleIdToken googleIdToken1 = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload1 = new GoogleIdToken.Payload();
        when(googleIdToken1.getPayload()).thenReturn(payload1);
        payload1.setEmail("user1@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token-1")).thenReturn(Optional.of(googleIdToken1));
        handler.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token-2");
        GoogleIdToken googleIdToken2 = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload2 = new GoogleIdToken.Payload();
        when(googleIdToken2.getPayload()).thenReturn(payload2);
        payload2.setEmail("user2@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token-2")).thenReturn(Optional.of(googleIdToken2));
        handler.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        assertAll(
            () -> assertEquals(2, handler.getEstimatedCacheSize()),
            () -> {
                handler.invalidateAllCache();
                assertEquals(0, handler.getEstimatedCacheSize());
            }
        );
    }

    @Test
    void testNoCacheWhenDisabledWithTTL() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        GoogleAuthenticationHandler handlerWithNoCache = context.registerInjectActivateService(
            GoogleAuthenticationHandler.class, Map.of("cache.ttl.seconds", 0)
        );
        handlerWithNoCache.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        handlerWithNoCache.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        assertAll(
            () -> verify(googleIdTokenVerifierProxy, times(2)).verify("test-id-token")
        );
    }

    @Test
    void testNoCacheWhenDisabledWithSize() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        GoogleAuthenticationHandler handlerWithNoCache = context.registerInjectActivateService(
            GoogleAuthenticationHandler.class, Map.of("cache.max-size", 0)
        );
        handlerWithNoCache.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        handlerWithNoCache.extractCredentials(request, new MockSlingJakartaHttpServletResponse());
        assertAll(
            () -> verify(googleIdTokenVerifierProxy, times(2)).verify("test-id-token")
        );
    }

    @Test
    @SuppressWarnings({"IllegalCatch", "PMD.AvoidCatchingGenericException"})
    void testFailActivation() {
        try {
            context.registerInjectActivateService(
                GoogleAuthenticationHandler.class, Map.of("cache.max-size", -1)
            );
        } catch (RuntimeException exception) {
            Throwable cause = exception.getCause().getCause();
            assertEquals(IllegalArgumentException.class, cause.getClass());
        }
        try {
            context.registerInjectActivateService(
                GoogleAuthenticationHandler.class, Map.of("cache.ttl.seconds", -1)
            );
        } catch (RuntimeException exception) {
            Throwable cause = exception.getCause().getCause();
            assertEquals(IllegalArgumentException.class, cause.getClass());
        }
    }

    @Test
    void testDropCredentials() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        String token = "test-token";
        request.addHeader(GoogleAuthenticationHandler.HEADER_NAME, token);
        request.setRemoteUser("test-user");

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("test@example.com");
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(googleIdTokenVerifierProxy.verify(token)).thenReturn(Optional.of(idToken));
        HttpServletResponse response = new MockSlingJakartaHttpServletResponse();

        // Populate cache - verify() should be called once
        assertNotNull(handler.extractCredentials(request, response));
        verify(googleIdTokenVerifierProxy, times(1)).verify(token);

        // Hit cache - verify() should still be called only once
        assertNotNull(handler.extractCredentials(request, response));
        verify(googleIdTokenVerifierProxy, times(1)).verify(token);

        // Drop credentials, which should invalidate the cache
        handler.dropCredentials(request, response);

        // Re-extract credentials - verify() should now be called a second time
        handler.extractCredentials(request, response);
        verify(googleIdTokenVerifierProxy, times(2)).verify(token);
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void testDropCredentialsWithIdentityProvider() {
        context.registerInjectActivateService(GoogleIdTokenVerifierProxy.class);
        GoogleIdentityProvider identityProvider = context.registerService(
            GoogleIdentityProvider.class, mock(GoogleIdentityProvider.class)
        );
        GoogleAuthenticationHandler handlerWithProvider = context.registerInjectActivateService(
            GoogleAuthenticationHandler.class
        );

        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        HttpServletResponse response = new MockSlingJakartaHttpServletResponse();
        String token = "test-token";
        request.addHeader(GoogleAuthenticationHandler.HEADER_NAME, token);
        String userEmail = "test@example.com";
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(userEmail);
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        when(googleIdTokenVerifierProxy.verify(token)).thenReturn(Optional.of(idToken));

        // Populate cache before dropping
        handlerWithProvider.extractCredentials(request, response);

        // Drop credentials
        handlerWithProvider.dropCredentials(request, response);

        // Verify that the user cache in the identity provider is invalidated
        verify(identityProvider).invalidateCacheForUser(userEmail);
    }

    @Test
    void testDropCredentialsNoToken() {
        context.registerInjectActivateService(GoogleIdTokenVerifierProxy.class);
        GoogleIdentityProvider identityProvider = context.registerService(
            GoogleIdentityProvider.class, mock(GoogleIdentityProvider.class)
        );
        GoogleAuthenticationHandler handlerWithProvider = context.registerInjectActivateService(
            GoogleAuthenticationHandler.class
        );

        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        HttpServletResponse response = new MockSlingJakartaHttpServletResponse();
        // No "X-ID-Token" header is set
        handlerWithProvider.dropCredentials(request, response);
        verify(identityProvider, never()).invalidateCacheForUser(anyString());
    }

    @Test
    @SuppressWarnings("LineLength")
    void testEmailMatch() {
        handler = context.registerInjectActivateService(
            GoogleAuthenticationHandler.class, Map.of("expected-email.regex", "^[A-Za-z0-9._%+-]+@example\\.com$")
        );
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        AuthenticationInfo authenticationInfo = Objects.requireNonNull(
            handler.extractCredentials(
                request, new MockSlingJakartaHttpServletResponse()
            )
        );
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
    @SuppressWarnings("LineLength")
    void testEmailNoMatch() {
        handler = context.registerInjectActivateService(
            GoogleAuthenticationHandler.class, Map.of("expected-email.regex", "^[A-Za-z0-9._%+-]+@example\\.com$")
        );
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@ciechanowiec.eu");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        assertNull(
            handler.extractCredentials(
                request, new MockSlingJakartaHttpServletResponse()
            )
        );
    }

    @Test
    @SuppressWarnings("LineLength")
    void testHostedDomainMatch() {
        handler = context.registerInjectActivateService(
            GoogleAuthenticationHandler.class, Map.of("expected-hosted-domain.regex", "ciechanowiec\\.eu")
        );
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        payload.setHostedDomain("ciechanowiec.eu");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        AuthenticationInfo authenticationInfo = Objects.requireNonNull(
            handler.extractCredentials(
                request, new MockSlingJakartaHttpServletResponse()
            )
        );
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
    @SuppressWarnings("LineLength")
    void testHostedDomainNoMatch() {
        handler = context.registerInjectActivateService(
            GoogleAuthenticationHandler.class, Map.of("expected-hosted-domain.regex", "ciechanowiec\\.eu")
        );
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        payload.setHostedDomain("example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        assertNull(
            handler.extractCredentials(
                request, new MockSlingJakartaHttpServletResponse()
            )
        );
    }

    @Test
    @SuppressWarnings("LineLength")
    void testHostedDomainNoClaim() {
        handler = context.registerInjectActivateService(
            GoogleAuthenticationHandler.class, Map.of("expected-hosted-domain.regex", "ciechanowiec\\.eu")
        );
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        assertNull(
            handler.extractCredentials(
                request, new MockSlingJakartaHttpServletResponse()
            )
        );
    }

    @Test
    @SuppressWarnings("LineLength")
    void testHostedDomainDefaultRegexWithHd() {
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        request.setHeader(GoogleAuthenticationHandler.HEADER_NAME, "test-id-token");
        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        when(googleIdToken.getPayload()).thenReturn(payload);
        payload.setEmail("user@example.com");
        payload.setHostedDomain("example.com");
        when(googleIdTokenVerifierProxy.verify("test-id-token")).thenReturn(Optional.of(googleIdToken));
        AuthenticationInfo authenticationInfo = Objects.requireNonNull(
            handler.extractCredentials(
                request, new MockSlingJakartaHttpServletResponse()
            )
        );
        assertAll(
            () -> assertTrue(
                authenticationInfo.toString().startsWith(
                    "{user.jcr.credentials=GoogleCredentials(email=user@example.com), sling.authType=GoogleAuth, user.name=user@example.com, user.password"
                )
            ),
            () -> assertEquals("test-id-token", new String(authenticationInfo.getPassword()))
        );
    }
}
