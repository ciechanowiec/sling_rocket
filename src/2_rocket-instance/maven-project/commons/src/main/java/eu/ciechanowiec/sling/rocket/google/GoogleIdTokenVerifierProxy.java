package eu.ciechanowiec.sling.rocket.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Proxy for {@link GoogleIdTokenVerifier}.
 */
@Component(
    service = GoogleIdTokenVerifierProxy.class,
    immediate = true
)
@Slf4j
@ToString
@Designate(ocd = GoogleIdTokenVerifierProxyConfig.class)
@ServiceDescription("Proxy for GoogleIdTokenVerifier")
public class GoogleIdTokenVerifierProxy {

    @ToString.Exclude
    private final AtomicReference<GoogleIdTokenVerifier> googleIdTokenVerifier;

    /**
     * Constructs an instance of this class.
     *
     * @param config {@link GoogleIdTokenVerifierProxyConfig} used by the constructed instance
     */
    @Activate
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    @SneakyThrows
    public GoogleIdTokenVerifierProxy(
        GoogleIdTokenVerifierProxyConfig config
    ) {
        this.googleIdTokenVerifier = new AtomicReference<>(buildGoogleIdTokenVerifier(config));
        log.info("{} initialized", this);
    }

    @Modified
    void configure(GoogleIdTokenVerifierProxyConfig config) throws GeneralSecurityException, IOException {
        log.debug("Configuring {}", this);
        this.googleIdTokenVerifier.set(buildGoogleIdTokenVerifier(config));
        log.debug("Configured {}", this);
    }

    private GoogleIdTokenVerifier buildGoogleIdTokenVerifier(
        GoogleIdTokenVerifierProxyConfig config
    ) throws GeneralSecurityException, IOException {
        String audience = config.audience();
        return new GoogleIdTokenVerifier.Builder(
            GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance()
        ).setAudience(List.of(audience)).build();
    }

    /**
     * Proxy for {@link GoogleIdTokenVerifier#verify(String)}.
     *
     * @param googleIdTokenString {@link GoogleIdToken} to be verified and represented as {@link String}
     * @return {@link Optional} containing the {@link GoogleIdToken} if the verification was successful; an empty
     * {@link Optional} is returned otherwise
     */
    @SuppressWarnings("WeakerAccess")
    public Optional<GoogleIdToken> verify(String googleIdTokenString) {
        try {
            return Optional.ofNullable(googleIdTokenVerifier.get().verify(googleIdTokenString));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
            log.debug("Couldn't verify token", exception);
            return Optional.empty();
        }
    }
}
