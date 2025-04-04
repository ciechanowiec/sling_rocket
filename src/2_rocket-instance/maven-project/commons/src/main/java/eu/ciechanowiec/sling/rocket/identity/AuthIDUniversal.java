package eu.ciechanowiec.sling.rocket.identity;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;

/**
 * ID of an {@link Authorizable}. Can be either ID of a {@link Group} or a {@link User}.
 */
public class AuthIDUniversal implements AuthID {

    private final String value;

    /**
     * Constructs an instance of this class.
     *
     * @param originalAuthID value of an ID of an {@link Authorizable} that will be represented by this
     *                       {@link AuthIDUniversal}
     */
    @SuppressWarnings("WeakerAccess")
    public AuthIDUniversal(String originalAuthID) {
        this.value = originalAuthID;
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    @SuppressWarnings({"SimplifiableIfStatement", "PMD.SimplifyBooleanReturns"})
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        return comparedObject instanceof AuthID comparedAuthID && this.get().equals(comparedAuthID.get());
    }

    @Override
    public int hashCode() {
        return value.hashCode() * 23;
    }

    @Override
    public int compareTo(AuthID comparedAuthID) {
        String comparedGroupIDValue = comparedAuthID.get();
        return value.compareTo(comparedGroupIDValue);
    }

    @Override
    public String toString() {
        return value;
    }
}
