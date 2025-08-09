package eu.ciechanowiec.sling.rocket.google;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link GoogleIdentityProvider}.
 */
@ObjectClassDefinition
@SuppressWarnings("TypeName")
public @interface GoogleIdentityProviderConfig {

    /**
     * Time to live (TTL) for the cached result of user/group retrieval in seconds, i.e., for the result produced by
     * {@link GoogleIdentityProvider#getUser(String)} and {@link GoogleIdentityProvider#getGroup(String)} for a specific
     * {@code userId} or group {@code name} respectively.
     * <p>
     * If the value is {@code 0}, then no caching is applied.
     * <p>
     * The value must not be lower than {@code 0}.
     *
     * @return time to live (TTL) for the cached result of user/group retrieval in seconds.
     */
    @AttributeDefinition(
        name = "Cache TTL Seconds",
        description = "Time to live (TTL) for the cached result of user/group retrieval in seconds",
        defaultValue = "1200",
        type = AttributeType.INTEGER,
        min = "0"
    )
    @SuppressWarnings({"squid:S100", "MagicNumber"})
    int cache_ttl_seconds() default 1200;

    /**
     * Maximum number of the cached results of user/group retrieval (i.e., for the result produced by
     * {@link GoogleIdentityProvider#getUser(String)} and {@link GoogleIdentityProvider#getGroup(String)} for a specific
     * {@code userId} or group {@code name} respectively stored in cache. The cache will not exceed this limit, and
     * older entries may be evicted when the cache size approaches this limit.
     * <p>
     * For user retrieval and for group retrieval, the specified cache size limit is counted individually and
     * separately. The exhaustion of one of those limits has no effect on the other one.
     * <p>
     * If the value is {@code 0}, then no caching is applied.
     * <p>
     * The value must not be lower than {@code 0}.
     *
     * @return maximum number of the cached results of the credentials extraction stored in cache.
     */
    @AttributeDefinition(
        name = "Cache Max Size",
        description = "Maximum number of the cached results of the credentials extraction stored in cache",
        defaultValue = "100000",
        type = AttributeType.INTEGER,
        min = "0"
    )
    @SuppressWarnings({"squid:S100", "MagicNumber"})
    int cache_max$_$size() default 100_000;
}
