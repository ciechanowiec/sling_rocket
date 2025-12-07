package eu.ciechanowiec.sling.rocket.google.auth.sling;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.auth.core.spi.JakartaAuthenticationHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link GoogleAuthenticationHandler}.
 */
@ObjectClassDefinition
@SuppressWarnings("TypeName")
public @interface GoogleAuthenticationHandlerConfig {

    /**
     * Values for the {@link JakartaAuthenticationHandler#PATH_PROPERTY} property.
     *
     * @return values for the {@link JakartaAuthenticationHandler#PATH_PROPERTY} property
     */
    @AttributeDefinition(
        name = "Path",
        description = "Values for the AuthenticationHandler `path` property",
        defaultValue = "/",
        type = AttributeType.STRING
    )
    String[] path() default "/";

    /**
     * Regular expression (regex) for the expected {@code hd} (hosted domain) claim of a {@link GoogleIdToken}.
     * <ol>
     *     <li>
     * The {@code hd} claim indicates the domain associated with the Google Workspace or Cloud organization of the user.
     * The claim is present only if the user belongs to a Google Workspace or Cloud organization.
     *     </li>
     *     <li>
     * This configuration property allows restricting authentication to users belonging to specific domains. The value
     * of the {@code hd} claim from the verified {@link GoogleIdToken} is matched against this regular expression. If
     * the value does not match, the authentication attempt will be rejected.
     *     </li>
     *     <li>
     * If the {@code hd} claim is not present in the {@link GoogleIdToken}, it is treated as an
     * {@link StringUtils#EMPTY} {@link String} for the purpose of this validation. Therefore, the authentication will
     * only pass if the configured regex matches an {@link StringUtils#EMPTY} {@link String}.
     *     </li>
     *     <li>
     * The default value is {@code ".*"}, which matches any value, including an {@link StringUtils#EMPTY}
     * {@link String}, thus allowing users both belonging and not belonging to some domain. To require a specific domain
     * like {@code "example.com"}, the value should be set to {@code "example\\.com"} (note that this would reject users
     * without the {@code hd} claim).
     *     </li>
     * </ol>
     *
     * @return regular expression (regex) for the expected {@code hd} (hosted domain) claim of a {@link GoogleIdToken}
     */
    @AttributeDefinition(
        name = "Regex for expected hosted domain (hd) claim",
        description = "Regular expression (regex) for the expected 'hd' (hosted domain) claim of a Google ID Token",
        defaultValue = ".*",
        type = AttributeType.STRING
    )
    String expected$_$hosted$_$domain_regex() default ".*";

    /**
     * Regular expression (regex) for the expected {@code email} claim of a {@link GoogleIdToken}.
     * <ol>
     *     <li>
     * The {@code email} claim contains the user's email address.
     *     </li>
     *     <li>
     * This configuration property allows restricting authentication to users with specific email addresses. The value
     * of the email claim from the verified {@link GoogleIdToken} is matched against this regular expression. If the
     * value does not match, the authentication attempt will be rejected.
     *     </li>
     *     <li>
     * The default value is {@code ".*"}, which matches any value, including an {@link StringUtils#EMPTY}
     * {@link String}, thus allowing users without email address restrictions. <p>
     * <i>Example:</i><p> To ensure that only users with email addresses from a domain {@code "example.com"} are allowed
     * the value should be set to {@code "^[A-Za-z0-9._%+-]+@example\\.com$"}.
     *     </li>
     * </ol>
     * @return regular expression (regex) for the expected {@code email} claim of a {@link GoogleIdToken}
     */
    String expected$_$email_regex() default ".*";

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
