package eu.ciechanowiec.sling.rocket.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.metatype.annotations.Designate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link AuthenticationHandler} for authenticating users via a {@link GoogleIdToken}.
 * <p>
 * This {@link AuthenticationHandler} extracts a {@link GoogleIdToken} from an HTTP header named
 * {@link GoogleAuthenticationHandler#HEADER_NAME}, verifies it using the {@link GoogleIdTokenVerifierProxy}, and then
 * creates an {@link AuthenticationInfo} object upon successful validation.
 */
@Component(
    service = {AuthenticationHandler.class, GoogleAuthenticationHandler.class},
    property = AuthenticationHandler.TYPE_PROPERTY + "=" + GoogleAuthenticationHandler.AUTH_TYPE,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@ServiceRanking(10_000)
@Slf4j
@ToString
@SuppressWarnings("TypeName")
@Designate(ocd = GoogleAuthenticationHandlerConfig.class)
public class GoogleAuthenticationHandler implements AuthenticationHandler {

    /**
     * The authentication type identifier for this {@link AuthenticationHandler}.
     */
    static final String AUTH_TYPE = "GoogleAuth";

    /**
     * The name of the HTTP header from which the {@link GoogleIdToken} token is extracted.
     */
    static final String HEADER_NAME = "X-ID-Token";
    private final GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy;
    private final AtomicReference<GoogleAuthenticationHandlerConfig> config;

    /**
     * Constructs an instance of this class.
     *
     * @param googleIdTokenVerifierProxy {@link GoogleIdTokenVerifierProxy} for verifying {@link GoogleIdToken}s
     * @param config                     {@link GoogleAuthenticationHandlerConfig} used by the constructed instance
     */
    @Activate
    public GoogleAuthenticationHandler(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy,
        GoogleAuthenticationHandlerConfig config
    ) {
        this.googleIdTokenVerifierProxy = googleIdTokenVerifierProxy;
        this.config = new AtomicReference<>(config);
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(GoogleAuthenticationHandlerConfig config) {
        this.config.set(config);
        log.info("Configured {}", this);
    }

    /**
     * Extracts {@link GoogleCredentials} from the provided {@link HttpServletRequest}.
     * <p>
     * It looks for a {@link GoogleIdToken} in the {@link GoogleAuthenticationHandler#HEADER_NAME} header in the
     * provided {@link HttpServletRequest}. If the {@link GoogleIdToken} is present and valid, an
     * {@link AuthenticationInfo} that describes valid {@link GoogleCredentials} is returned. Otherwise, a {@code null}
     * is returned.
     *
     * @param request  {@link HttpServletRequest} from which the {@link GoogleCredentials} must be extracted
     * @param response {@link HttpServletResponse} in the chain
     * @return {@link AuthenticationInfo} that describes valid {@link GoogleCredentials} if the {@link GoogleIdToken} is
     * present and valid; a {@code null} is returned otherwise
     */
    @Override
    @SuppressWarnings({"ReturnOfNull", "Regexp"})
    public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response) {
        log.trace(
            "Extracting credentials from the request to '{}'", request.getRequestURI()
        );
        return Optional.ofNullable(request.getHeader(HEADER_NAME))
            .flatMap(this::extractCredentials)
            .orElseGet(
                () -> {
                    request.setAttribute(
                        FAILURE_REASON, "Unable to extract credentials from the request"
                    );
                    return null;
                }
            );
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private Optional<AuthenticationInfo> extractCredentials(String googleIdToken) {
        return googleIdTokenVerifierProxy.verify(googleIdToken)
            .map(GoogleIdToken::getPayload)
            .map(GoogleIdToken.Payload::getEmail)
            .map(
                email -> {
                    log.trace(
                        "Will create {} of type {} for '{}'", AuthenticationInfo.class.getSimpleName(), AUTH_TYPE, email
                    );
                    return email;
                }
            ).map(
                email -> {
                    AuthenticationInfo authenticationInfo = new AuthenticationInfo(
                        AUTH_TYPE, email, googleIdToken.toCharArray()
                    );
                    authenticationInfo.put(
                        JcrResourceConstants.AUTHENTICATION_INFO_CREDENTIALS,
                        new GoogleCredentials(email, googleIdToken.toCharArray())
                    );
                    return authenticationInfo;
                }
            );
    }

    /**
     * This {@link GoogleAuthenticationHandler} does not support challenging the user for credentials. It assumes the
     * credentials ({@link GoogleIdToken}) are already present in the {@link HttpServletRequest}.
     *
     * @param request  {@link HttpServletRequest} in the chain
     * @param response {@link HttpServletResponse} in the chain
     * @return always {@code false}
     */
    @Override
    public boolean requestCredentials(HttpServletRequest request, HttpServletResponse response) {
        return false;
    }

    /**
     * This {@link GoogleAuthenticationHandler} does not support dropping credentials. No action is performed upon this
     * method call.
     *
     * @param request  {@link HttpServletRequest} in the chain
     * @param response {@link HttpServletResponse} in the chain
     */
    @Override
    public void dropCredentials(HttpServletRequest request, HttpServletResponse response) {
        log.debug("Dropping credentials for '{}'", request.getRequestURI());
    }
}
