package eu.ciechanowiec.sling.rocket.google.auth.sling;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import eu.ciechanowiec.sling.rocket.google.GoogleCredentials;
import org.apache.jackrabbit.oak.api.jmx.Description;
import org.apache.jackrabbit.oak.api.jmx.Name;
import org.apache.sling.auth.core.spi.AuthenticationInfo;

import java.util.Optional;

/**
 * MBean for a {@link GoogleAuthenticationHandler}.
 */
@Description(GoogleAuthenticationHandler.SERVICE_DESCRIPTION)
@SuppressWarnings({"TypeName", "PMD.LooseCoupling"})
public interface GoogleAuthenticationHandlerMBean {

    /**
     * Extracts {@link GoogleCredentials} from the provided {@link GoogleIdToken}.
     * <p>
     * If the {@link GoogleIdToken} is valid, an {@link Optional} containing the {@link AuthenticationInfo} that
     * describes valid {@link GoogleCredentials} is returned. Otherwise, an empty {@link Optional} is returned.
     *
     * @param googleIdToken {@link GoogleIdToken} from which the {@link GoogleCredentials} must be extracted
     * @return {@link Optional} containing the {@link AuthenticationInfo} that describes valid {@link GoogleCredentials}
     * if the {@link GoogleIdToken} is present and valid; an empty {@link Optional} is returned otherwise
     */
    @Description("Extracts GoogleCredentials from the provided GoogleIdToken")
    Optional<AuthenticationInfo> extractCredentials(
        @Name("googleIdToken")
        String googleIdToken
    );

    /**
     * Invalidates all cache entries.
     *
     * @return estimated number of valid cache entries that existed before the invalidation
     */
    @Description(
        "Invalidates all cache entries. "
            + "Returns the estimated number of all valid cache entries existing before invalidation"
    )
    long invalidateAllCache();

    /**
     * Show the estimated number of all valid cache entries.
     *
     * @return estimated number of all valid cache entries
     */
    @Description("Show the estimated number of all valid cache entries")
    long getEstimatedCacheSize();
}
