package eu.ciechanowiec.sling.rocket.identity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    @SuppressWarnings("SimplifiableIfStatement")
    @SuppressFBWarnings({"EC_UNRELATED_TYPES", "EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS"})
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        if (comparedObject instanceof AuthIDGroup) {
            return false;
        }
        return comparedObject instanceof AuthID comparedAuthID && this.get().equals(comparedAuthID.get());
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
