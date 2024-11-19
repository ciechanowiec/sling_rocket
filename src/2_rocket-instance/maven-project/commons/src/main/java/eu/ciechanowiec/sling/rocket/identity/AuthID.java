package eu.ciechanowiec.sling.rocket.identity;

import org.apache.jackrabbit.api.security.user.Authorizable;

/**
 * ID of an {@link Authorizable}.
 */
public interface AuthID extends Comparable<AuthID> {

    /**
     * Value of an ID of {@link Authorizable}.
     * @return value of an ID of {@link Authorizable}
     */
    String get();
}
