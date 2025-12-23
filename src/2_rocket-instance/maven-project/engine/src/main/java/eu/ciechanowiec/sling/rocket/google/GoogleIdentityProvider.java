package eu.ciechanowiec.sling.rocket.google;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.services.directory.Directory;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.apache.jackrabbit.oak.spi.security.authentication.credentials.CredentialsSupport;
import org.apache.jackrabbit.oak.spi.security.authentication.external.*;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import javax.jcr.Credentials;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link ExternalIdentityProvider} based on Google {@link Directory}.
 */
@Component(
    service = {
        ExternalIdentityProvider.class, GoogleIdentityProvider.class,
        GoogleIdentityProviderMBean.class, CredentialsSupport.class
    },
    immediate = true,
    property = "jmx.objectname=eu.ciechanowiec.sling.rocket.engine:type=Identity Management,name=Google Identity Provider"
)
@SuppressWarnings(
    {
        "TypeName", "NullableProblems", "PMD.LooseCoupling", "ClassWithTooManyMethods", "MethodCount",
        "PMD.CouplingBetweenObjects", "MatchXpath", "LineLength"
    }
)
@ServiceDescription(GoogleIdentityProvider.SERVICE_DESCRIPTION)
@Designate(ocd = GoogleIdentityProviderConfig.class)
@ToString
@Slf4j
public class GoogleIdentityProvider extends AnnotatedStandardMBean
    implements ExternalIdentityProvider, GoogleIdentityProviderMBean, CredentialsSupport {

    static final String SERVICE_DESCRIPTION = "External Identity Provider based on Google Directory";
    private final GoogleDirectory googleDirectory;
    private final GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy;
    @ToString.Exclude
    private final AtomicReference<Cache<String, Optional<ExternalUser>>> usersCache;
    @ToString.Exclude
    private final AtomicReference<Cache<String, Optional<ExternalGroup>>> groupsCache;
    private final AtomicReference<GoogleIdentityProviderConfig> config;

    /**
     * Constructs an instance of this class.
     *
     * @param googleDirectory            {@link GoogleDirectory} used by the constructed instance
     * @param googleIdTokenVerifierProxy {@link GoogleIdTokenVerifierProxy} used by the constructed instance
     * @param config                     {@link GoogleIdentityProviderConfig} used by the constructed instance
     */
    @Activate
    public GoogleIdentityProvider(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        GoogleDirectory googleDirectory,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy,
        GoogleIdentityProviderConfig config
    ) {
        super(GoogleIdentityProviderMBean.class);
        this.googleDirectory = googleDirectory;
        this.googleIdTokenVerifierProxy = googleIdTokenVerifierProxy;
        this.usersCache = new AtomicReference<>(buildCache(config, ExternalUser.class));
        this.groupsCache = new AtomicReference<>(buildCache(config, ExternalGroup.class));
        this.config = new AtomicReference<>(config);
        log.info("{} initialized", this);
    }

    @Modified
    void configure(GoogleIdentityProviderConfig config) {
        log.debug("Configuring {}", this);
        this.usersCache.set(buildCache(config, ExternalUser.class));
        this.groupsCache.set(buildCache(config, ExternalGroup.class));
        this.config.set(config);
        log.debug("Configured {}", this);
    }

    private boolean isCacheEnabled() {
        int cacheMaxSize = config.get().cache_max$_$size();
        int cacheTTLSeconds = config.get().cache_ttl_seconds();
        return cacheMaxSize > NumberUtils.INTEGER_ZERO && cacheTTLSeconds > NumberUtils.INTEGER_ZERO;
    }

    private <T> Cache<String, Optional<T>> buildCache(GoogleIdentityProviderConfig config, Class<T> cacheType) {
        long cacheTTLSeconds = config.cache_ttl_seconds();
        long cacheMaxSize = config.cache_max$_$size();
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        builder.expireAfterWrite(cacheTTLSeconds, TimeUnit.SECONDS);
        builder.maximumSize(cacheMaxSize);
        log.info("Cache built. TTL: {} seconds. Max size: {}. Type: {}", cacheTTLSeconds, cacheMaxSize, cacheType);
        return builder.build();
    }

    @Override
    public String getName() {
        return GoogleIdentityProvider.class.getSimpleName();
    }

    @Override
    public ExternalIdentity getIdentity(ExternalIdentityRef ref) {
        log.trace("getIdentity({})", ref);
        return Optional.ofNullable(ref.getProviderName())
            .filter(providerNameFromEIR -> providerNameFromEIR.equals(getName()))
            .map(providerNameFromEIR -> ref.getId())
            .flatMap(
                externalIdentityRefID -> googleDirectory.retrieveUser(externalIdentityRefID)
                    .<ExternalIdentity>map(user -> new GoogleExternalUser(user, googleDirectory))
                    .or(
                        () -> googleDirectory.retrieveGroup(externalIdentityRefID)
                            .map(group -> new GoogleExternalGroup(group, googleDirectory))
                    )
            ).orElse(null);
    }

    /**
     * Returns the {@link ExternalUser} for the specified {@link User#getID()}. If the {@link User} does not exist,
     * {@code null} is returned.
     * <p>
     * The results of the {@link User} retrieval might be cached according to the configuration of this service.
     *
     * @param userId {@link User#getID()} for the requested {@link User}
     * @return {@link ExternalUser} for the specified {@link User#getID()}; if the {@link User} does not exist,
     * {@code null} is returned
     */
    @Override
    @Nullable
    public ExternalUser getUser(String userId) {
        log.trace("getUser({})", userId);
        return getCachedUser(userId)
            .orElse(null);
    }

    private Optional<ExternalUser> getCachedUser(String userId) {
        return Optional.of(usersCache.get())
            .filter(cache -> isCacheEnabled())
            .map(
                cache -> cache.get(
                    userId, key -> googleDirectory.retrieveUser(userId)
                        .map(user -> new GoogleExternalUser(user, googleDirectory))
                )
            ).orElseGet(
                () -> googleDirectory.retrieveUser(userId)
                    .map(user -> new GoogleExternalUser(user, googleDirectory))
            );
    }

    @Override
    public ExternalUser authenticate(Credentials credentials) {
        log.trace("Authenticating {}", credentials);
        return Optional.of(credentials)
            .filter(GoogleCredentials.class::isInstance)
            .map(GoogleCredentials.class::cast)
            .flatMap(this::authenticate)
            .orElse(null);
    }

    private Optional<ExternalUser> authenticate(GoogleCredentials googleCredentials) {
        String actualEmail = googleCredentials.email();
        return extractPayload(googleCredentials).map(GoogleIdToken.Payload::getEmail)
            .filter(extractedEmail -> extractedEmail.equals(actualEmail))
            .map(this::getUser);
    }

    @Override
    public Optional<ExternalUser> authenticate(String email, String idToken) {
        Credentials credentials = new GoogleCredentials(email, idToken.toCharArray());
        return Optional.ofNullable(authenticate(credentials));
    }

    private Optional<GoogleIdToken.Payload> extractPayload(
        GoogleCredentials googleCredentials
    ) {
        log.trace("Extracting payload from {}", googleCredentials);
        String actualIDToken = googleCredentials.idToken();
        return googleIdTokenVerifierProxy.verify(actualIDToken)
            .map(GoogleIdToken::getPayload)
            .map(
                payload -> {
                    log.trace("From {} this payload extracted: {}", googleCredentials, payload);
                    return payload;
                }
            );
    }

    /**
     * Returns the {@link ExternalGroup} for the specified {@link Group#getID()}. If the {@link Group} does not exist,
     * {@code null} is returned.
     * <p>
     * The results of the {@link Group} retrieval might be cached according to the configuration of this service.
     *
     * @param name {@link Group#getID()} for the requested {@link Group}
     * @return {@link ExternalGroup} for the specified {@link Group#getID()}; if the {@link Group} does not exist,
     * {@code null} is returned
     */
    @Override
    public ExternalGroup getGroup(String name) {
        log.trace("getGroup({})", name);
        return getCachedGroup(name)
            .orElse(null);
    }

    private Optional<ExternalGroup> getCachedGroup(String name) {
        return Optional.of(groupsCache.get())
            .filter(cache -> isCacheEnabled())
            .map(
                cache -> cache.get(
                    name, key -> googleDirectory.retrieveGroup(name)
                        .map(group -> new GoogleExternalGroup(group, googleDirectory))
                )
            ).orElseGet(
                () -> googleDirectory.retrieveGroup(name)
                    .map(group -> new GoogleExternalGroup(group, googleDirectory))
            );
    }

    @Override
    public Iterator<ExternalUser> listUsers() {
        return googleDirectory.listUsers()
            .stream()
            .<ExternalUser>map(user -> new GoogleExternalUser(user, googleDirectory))
            .toList()
            .iterator();
    }

    @Override
    public Iterator<ExternalGroup> listGroups() {
        return googleDirectory.listGroups()
            .stream()
            .<ExternalGroup>map(group -> new GoogleExternalGroup(group, googleDirectory))
            .toList()
            .iterator();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set<Class> getCredentialClasses() {
        return Set.of(GoogleCredentials.class);
    }

    @Override
    @Nullable
    public String getUserId(Credentials credentials) {
        return Optional.of(credentials)
            .filter(GoogleCredentials.class::isInstance)
            .map(GoogleCredentials.class::cast)
            .map(GoogleCredentials::email)
            .orElse(null);
    }

    @Override
    public Map<String, ?> getAttributes(Credentials credentials) {
        return Map.of();
    }

    @Override
    public boolean setAttributes(Credentials credentials, Map<String, ?> attributes) {
        return false;
    }

    @Override
    public long invalidateAllCache() {
        long estimatedSizeUsers = usersCache.get().estimatedSize();
        long estimatedSizeGroups = groupsCache.get().estimatedSize();
        long estimatedSize = estimatedSizeUsers + estimatedSizeGroups;
        usersCache.get().invalidateAll();
        groupsCache.get().invalidateAll();
        log.info("Cache invalidated. Estimated size before invalidation: {}", estimatedSize);
        return estimatedSize;
    }

    @Override
    public void invalidateCacheForUser(String userId) {
        log.trace("Invalidating cache for user '{}'", userId);
        usersCache.get().invalidate(userId);
    }

    @Override
    public long getEstimatedCacheSizeForUsers() {
        usersCache.get().cleanUp();
        return usersCache.get().estimatedSize();
    }

    @Override
    public long getEstimatedCacheSizeForGroups() {
        groupsCache.get().cleanUp();
        return groupsCache.get().estimatedSize();
    }
}
