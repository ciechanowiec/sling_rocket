package eu.ciechanowiec.sling.rocket.commons;

import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Repository;

/**
 * Provides access to Apache Sling resources, including the underlying {@link Repository}, for a specific {@link User}.
 * The scope of the provided access is equal to the scope of the access configured for that {@link User}.
 */
public class UserResourceAccess implements ResourceAccess {

    private final AuthIDUser authIDUser;
    private final FullResourceAccess fullResourceAccess;

    /**
     * Constructs an instance of this class.
     * @param authIDUser {@link AuthIDUser} representing the {@link User} for which the resource access will be provided
     * @param fullResourceAccess {@link FullResourceAccess} that will be used to acquire access to resources
     */
    @SuppressWarnings("WeakerAccess")
    public UserResourceAccess(AuthIDUser authIDUser, FullResourceAccess fullResourceAccess) {
        this.authIDUser = authIDUser;
        this.fullResourceAccess = fullResourceAccess;
    }

    /**
     * Returns a {@link ResourceResolver} that provides access to Apache Sling resources for the {@link User} wrapped
     * in this {@link UserResourceAccess}. The scope of the access provided by the returned {@link ResourceResolver}
     * is equal to the scope of the access configured for the {@link User}.
     * @return {@link ResourceResolver} that provides access to Apache Sling resources
     */
    @Override
    public ResourceResolver acquireAccess() {
        return fullResourceAccess.acquireAccess(authIDUser);
    }
}
