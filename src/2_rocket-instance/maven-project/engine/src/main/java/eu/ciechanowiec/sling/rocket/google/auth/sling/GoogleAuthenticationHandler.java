package eu.ciechanowiec.sling.rocket.google.auth.sling;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import eu.ciechanowiec.sling.rocket.google.GoogleCredentials;
import eu.ciechanowiec.sling.rocket.google.GoogleIdTokenVerifierProxy;
import eu.ciechanowiec.sling.rocket.google.GoogleIdentityProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalUser;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.JakartaAuthenticationHandler;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.metatype.annotations.Designate;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link JakartaAuthenticationHandler} for authenticating users via a {@link GoogleIdToken}.
 * <p>
 * This {@link JakartaAuthenticationHandler} extracts a {@link GoogleIdToken} from an HTTP header named
 * {@link GoogleAuthenticationHandler#HEADER_NAME}, verifies it using the {@link GoogleIdTokenVerifierProxy}, and then
 * creates an {@link AuthenticationInfo} object upon successful validation.
 */
@Component(
    service = {
        JakartaAuthenticationHandler.class, GoogleAuthenticationHandler.class, GoogleAuthenticationHandlerMBean.class
    },
    property = {
        JakartaAuthenticationHandler.TYPE_PROPERTY + "=" + GoogleAuthenticationHandler.AUTH_TYPE,
        "jmx.objectname=eu.ciechanowiec.sling.rocket.engine:type=Authentication,name=Google Authentication Handler"
    },
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@ServiceRanking(10_000)
@Slf4j
@ToString
@SuppressWarnings({"TypeName", "PMD.LooseCoupling"})
@Designate(ocd = GoogleAuthenticationHandlerConfig.class)
@ServiceDescription(GoogleAuthenticationHandler.SERVICE_DESCRIPTION)
public class GoogleAuthenticationHandler extends AnnotatedStandardMBean
    implements JakartaAuthenticationHandler, GoogleAuthenticationHandlerMBean {

    static final String SERVICE_DESCRIPTION = "AuthenticationHandler for authenticating users via a GoogleIdToken";

    /**
     * The authentication type identifier for this {@link JakartaAuthenticationHandler}.
     */
    static final String AUTH_TYPE = "GoogleAuth";

    /**
     * The name of the HTTP header from which the {@link GoogleIdToken} token is extracted.
     */
    static final String HEADER_NAME = "X-ID-Token";
    private final GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<GoogleIdentityProvider> googleIdentityProviderNullable;
    @ToString.Exclude
    private final AtomicReference<Cache<String, Optional<AuthenticationInfo>>> credentialsExtractionCache;
    @ToString.Exclude
    private final AtomicReference<GoogleAuthenticationHandlerConfig> config;

    /**
     * Constructs an instance of this class.
     *
     * @param googleIdTokenVerifierProxy {@link GoogleIdTokenVerifierProxy} for verifying {@link GoogleIdToken}s
     * @param googleIdentityProvider     {@link GoogleIdentityProvider} that delivers related {@link ExternalUser}s
     * @param config                     {@link GoogleAuthenticationHandlerConfig} used by the constructed instance
     */
    @Activate
    public GoogleAuthenticationHandler(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy,
        @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY
        )
        GoogleIdentityProvider googleIdentityProvider,
        GoogleAuthenticationHandlerConfig config
    ) {
        super(GoogleAuthenticationHandlerMBean.class);
        this.googleIdTokenVerifierProxy = googleIdTokenVerifierProxy;
        this.googleIdentityProviderNullable = Optional.ofNullable(googleIdentityProvider);
        this.credentialsExtractionCache = new AtomicReference<>(buildCache(config));
        this.config = new AtomicReference<>(config);
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(GoogleAuthenticationHandlerConfig config) {
        log.debug("Configuring {}", this);
        this.credentialsExtractionCache.set(buildCache(config));
        this.config.set(config);
        log.debug("Configured {}", this);
    }

    private Cache<String, Optional<AuthenticationInfo>> buildCache(GoogleAuthenticationHandlerConfig config) {
        long cacheTTLSeconds = config.cache_ttl_seconds();
        long cacheMaxSize = config.cache_max$_$size();
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        builder.expireAfterWrite(cacheTTLSeconds, TimeUnit.SECONDS);
        builder.maximumSize(cacheMaxSize);
        log.info("Cache built. TTL: {} seconds. Max size: {}", cacheTTLSeconds, cacheMaxSize);
        return builder.build();
    }

    /**
     * Extracts {@link GoogleCredentials} from the provided {@link HttpServletRequest}.
     * <p>
     * It looks for a {@link GoogleIdToken} in the {@link GoogleAuthenticationHandler#HEADER_NAME} header in the
     * provided {@link HttpServletRequest}. If the {@link GoogleIdToken} is present and valid, an
     * {@link AuthenticationInfo} that describes valid {@link GoogleCredentials} is returned. Otherwise, a {@code null}
     * is returned.
     * <p>
     * The results of the credentials extraction logic might be cached according to the configuration of this service.
     *
     * @param request  {@link HttpServletRequest} from which the {@link GoogleCredentials} must be extracted
     * @param response {@link HttpServletResponse} in the chain
     * @return {@link AuthenticationInfo} that describes valid {@link GoogleCredentials} if the {@link GoogleIdToken} is
     * present and valid; a {@code null} is returned otherwise
     */
    @Override
    @SuppressWarnings({"ReturnOfNull", "Regexp"})
    @Nullable
    public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.trace(
            "Extracting credentials from the request to '{}'", requestURI
        );
        return Optional.ofNullable(request.getHeader(HEADER_NAME))
            .flatMap(this::getCachedCredentials)
            .map(
                authenticationInfo -> {
                    log.trace("Extracted credentials from the request to '{}'", requestURI);
                    return authenticationInfo;
                }
            ).orElseGet(
                () -> {
                    request.setAttribute(
                        FAILURE_REASON, "Unable to extract credentials from the request"
                    );
                    return null;
                }
            );
    }

    @SuppressWarnings("PMD.LooseCoupling")
    @Override
    public Optional<AuthenticationInfo> extractCredentials(String googleIdToken) {
        return googleIdTokenVerifierProxy.verify(googleIdToken)
            .map(GoogleIdToken::getPayload)
            .filter(
                payload -> {
                    String expectedHostedDomainRegex = config.get().expected$_$hosted$_$domain_regex();
                    String actualHostedDomain = Optional.ofNullable(payload.getHostedDomain())
                        .orElse(StringUtils.EMPTY);
                    return actualHostedDomain.matches(expectedHostedDomainRegex);
                }
            ).map(GoogleIdToken.Payload::getEmail)
            .filter(
                email -> {
                    String expectedEmailRegex = config.get().expected$_$email_regex();
                    return email.matches(expectedEmailRegex);
                }
            ).map(
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

    private Optional<AuthenticationInfo> getCachedCredentials(String googleIdToken) {
        int cacheMaxSize = config.get().cache_max$_$size();
        int cacheTTLSeconds = config.get().cache_ttl_seconds();
        boolean isCacheEnabled = cacheMaxSize > NumberUtils.INTEGER_ZERO && cacheTTLSeconds > NumberUtils.INTEGER_ZERO;
        return Optional.of(credentialsExtractionCache.get())
            .filter(cache -> isCacheEnabled)
            .map(cache -> cache.get(googleIdToken, this::extractCredentials))
            .orElseGet(() -> extractCredentials(googleIdToken));
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

    @Override
    public void dropCredentials(HttpServletRequest request, HttpServletResponse response) {
        String remoteUser = request.getRemoteUser();
        log.debug("Dropping credentials for '{}'", remoteUser);
        Optional.ofNullable(extractCredentials(request, response))
            .map(AuthenticationInfo::getUser)
            .ifPresent(
                user -> googleIdentityProviderNullable.ifPresent(
                    googleIdentityProvider -> googleIdentityProvider.invalidateCacheForUser(user)
                )
            );
        Optional.ofNullable(request.getHeader(HEADER_NAME))
            .ifPresentOrElse(
                googleIdToken -> {
                    credentialsExtractionCache.get().invalidate(googleIdToken);
                    log.debug("Cached credentials invalidated for '{}'", remoteUser);
                },
                () -> log.debug("No cached credentials found for '{}'. Nothing to drop", remoteUser)
            );
    }

    @Override
    public long invalidateAllCache() {
        long estimatedSize = credentialsExtractionCache.get().estimatedSize();
        credentialsExtractionCache.get().invalidateAll();
        log.info("Cache invalidated. Estimated size before invalidation: {}", estimatedSize);
        return estimatedSize;
    }

    @Override
    public long getEstimatedCacheSize() {
        credentialsExtractionCache.get().cleanUp();
        return credentialsExtractionCache.get().estimatedSize();
    }
}
