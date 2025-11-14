package eu.ciechanowiec.sling.rocket.identity;

import org.apache.jackrabbit.api.security.user.Group;

/**
 * ID of a {@link Group}.
 */
public class AuthIDGroup implements AuthID {

    private final AuthIDUniversal authIDUniversal;

    /**
     * Constructs an instance of this class.
     *
     * @param originalAuthID value of an ID of a {@link Group} that will be represented by this {@link AuthIDGroup}
     */
    @SuppressWarnings("WeakerAccess")
    public AuthIDGroup(String originalAuthID) {
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
        return comparedObject instanceof AuthIDGroup comparedAuthID && this.get().equals(comparedAuthID.get());
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
