package eu.ciechanowiec.sling.rocket.google;

import org.apache.jackrabbit.oak.api.jmx.Description;
import org.apache.jackrabbit.oak.api.jmx.Name;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalUser;

import java.util.Optional;

/**
 * MBean for a {@link GoogleIdentityProvider}.
 */
@FunctionalInterface
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
    @Description("Authenticate the External User represented by the specified credentials")
    Optional<ExternalUser> authenticate(
        @Name("email")
        String email,
        @Name("idToken")
        String idToken
    );
}
