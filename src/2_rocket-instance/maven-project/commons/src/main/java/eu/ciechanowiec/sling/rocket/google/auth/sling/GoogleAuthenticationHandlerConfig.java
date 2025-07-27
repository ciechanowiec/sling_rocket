package eu.ciechanowiec.sling.rocket.google.auth.sling;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Configuration for {@link GoogleAuthenticationHandler}.
 */
@ObjectClassDefinition
@SuppressWarnings("TypeName")
public @interface GoogleAuthenticationHandlerConfig {

    /**
     * Values for the {@link AuthenticationHandler#PATH_PROPERTY} property.
     *
     * @return values for the {@link AuthenticationHandler#PATH_PROPERTY} property
     */
    @AttributeDefinition(
        name = "Path",
        description = "Values for the AuthenticationHandler `path` property",
        defaultValue = "/",
        type = AttributeType.STRING
    )
    String[] path() default "/";

    /**
     * Time to live (TTL) for the cached result of the credentials extraction in seconds, i.e., for the result produced
     * by {@link GoogleAuthenticationHandler#extractCredentials(HttpServletRequest, HttpServletResponse)} for a specific
     * {@link GoogleIdToken}.
     * <p>
     * If the value is {@code 0}, then no caching is applied.
     * <p>
     * The value must not be lower than {@code 0}.
     *
     * @return time to live (TTL) for the cached result of the credentials extraction in seconds.
     */
    @AttributeDefinition(
        name = "Cache TTL Seconds",
        description = "Time to live (TTL) for the cached result of the credentials extraction in seconds",
        defaultValue = "1200",
        type = AttributeType.INTEGER,
        min = "0"
    )
    @SuppressWarnings({"squid:S100", "MagicNumber"})
    int cache_ttl_seconds() default 1200;

    /**
     * Maximum number of the cached results of the credentials extraction (i.e., results produced by
     * {@link GoogleAuthenticationHandler#extractCredentials(HttpServletRequest, HttpServletResponse)} for a specific
     * {@link GoogleIdToken}) stored in cache. The cache will not exceed this limit, and older entries may be evicted
     * when the cache size approaches this limit.
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
