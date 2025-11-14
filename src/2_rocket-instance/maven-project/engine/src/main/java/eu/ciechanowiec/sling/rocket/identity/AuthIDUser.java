package eu.ciechanowiec.sling.rocket.identity;

import org.apache.jackrabbit.api.security.user.User;

/**
 * ID of a {@link User}.
 */
public class AuthIDUser implements AuthID {

    private final AuthIDUniversal authIDUniversal;

    /**
     * Constructs an instance of this class.
     *
     * @param originalAuthID value of an ID of a {@link User} that will be represented by this {@link AuthIDUser}
     */
    @SuppressWarnings("WeakerAccess")
    public AuthIDUser(String originalAuthID) {
        authIDUniversal = new AuthIDUniversal(originalAuthID);
    }

    @Override
    public String get() {
        return authIDUniversal.get();
    }

    @Override
    @SuppressWarnings({"SimplifiableIfStatement", "PMD.SimplifyBooleanReturns"})
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        return comparedObject instanceof AuthIDUser comparedAuthID && this.get().equals(comparedAuthID.get());
    }

    @Override
    public int hashCode() {
        return authIDUniversal.hashCode();
    }

    @Override
    public String toString() {
        return authIDUniversal.toString();
    }

    @Override
    public int compareTo(
        @SuppressWarnings("NullableProblems")
        AuthID comparedAuthID
    ) {
        return authIDUniversal.compareTo(comparedAuthID);
    }
}
