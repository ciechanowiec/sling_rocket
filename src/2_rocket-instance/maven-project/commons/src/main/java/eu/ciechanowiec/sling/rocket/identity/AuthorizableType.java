package eu.ciechanowiec.sling.rocket.identity;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;

/**
 * Type of {@link Authorizable}.
 */
@SuppressWarnings("unused")
public enum AuthorizableType {

    /**
     * {@link Authorizable} of type {@link User}.
     */
    USER,

    /**
     * {@link Authorizable} of type {@link Group}.
     */
    GROUP
}
