package eu.ciechanowiec.sling.rocket.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.ToString;

import javax.jcr.Credentials;

/**
 * {@link Credentials} based on an email address and respective {@link GoogleIdToken}.
 */
@ToString
@SuppressWarnings("WeakerAccess")
public class GoogleCredentials implements Credentials {

    /**
     * The email address associated with the {@link Credentials}.
     */
    private final String email;

    /**
     * The {@link GoogleIdToken} associated with the {@link Credentials}.
     */
    @ToString.Exclude
    private final char[] idToken;

    /**
     * Constructs an instance of this class.
     *
     * @param email   email address associated with the {@link Credentials}
     * @param idToken {@link GoogleIdToken} associated with the {@link Credentials}
     */
    @SuppressWarnings("PMD.UseVarargs")
    public GoogleCredentials(String email, char[] idToken) {
        this.email = email;
        this.idToken = idToken.clone();
    }

    String email() {
        return email;
    }

    String idToken() {
        return new String(idToken);
    }
}
