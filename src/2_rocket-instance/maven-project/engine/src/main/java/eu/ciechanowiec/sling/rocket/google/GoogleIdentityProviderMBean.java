package eu.ciechanowiec.sling.rocket.google;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.oak.api.jmx.Description;
import org.apache.jackrabbit.oak.api.jmx.Name;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalUser;

import java.util.Optional;

/**
 * MBean for a {@link GoogleIdentityProvider}.
 */
@Description(GoogleIdentityProvider.SERVICE_DESCRIPTION)
@SuppressWarnings({"TypeName", "WeakerAccess"})
public interface GoogleIdentityProviderMBean {

    /**
     * Authenticate the {@link ExternalUser} represented by the specified credentials.
     *
     * @param email   email address of the {@link ExternalUser} to authenticate
     * @param idToken Google OAuth ID token to validate
     * @return an {@link Optional} containing the authenticated {@link ExternalUser}; an empty {@link Optional} is
     * returned if authentication failed
     */
    @SuppressWarnings("unused")
    @Description("Authenticate the External User represented by the specified credentials")
    Optional<ExternalUser> authenticate(
        @Name("email")
        String email,
        @Name("idToken")
        String idToken
    );

    /**
     * Invalidates all cache entries.
     *
     * @return estimated number of valid cache entries that existed before the invalidation
     */
    @SuppressWarnings("UnusedReturnValue")
    @Description(
        "Invalidates all cache entries. "
            + "Returns the estimated number of all valid cache entries existing before invalidation"
    )
    long invalidateAllCache();

    /**
     * Show the estimated number of all valid cache entries produced by {@link GoogleIdentityProvider#getUser(String)}.
     *
     * @return estimated number of all valid cache entries produced by {@link GoogleIdentityProvider#getUser(String)}
     */
    @Description("Show the estimated number of all valid cache entries produced for users")
    long getEstimatedCacheSizeForUsers();

    /**
     * Show the estimated number of all valid cache entries produced by
     * {@link GoogleIdentityProvider#getGroup(String)}.
     *
     * @return estimated number of all valid cache entries produced by {@link GoogleIdentityProvider#getGroup(String)}
     */
    @Description("Show the estimated number of all valid cache entries produced for groups")
    long getEstimatedCacheSizeForGroups();

    /**
     * Invalidates all cache entries produced by {@link GoogleIdentityProvider#getUser(String)} for the specified for
     * the specified {@link User#getID()}.
     *
     * @param userId {@link User#getID()} for the {@link User} whose cache should be invalidated
     */
    @Description("Invalidates all cache entries produced for the specified user")
    void invalidateCacheForUser(
        @Name("userId")
        String userId
    );
}
