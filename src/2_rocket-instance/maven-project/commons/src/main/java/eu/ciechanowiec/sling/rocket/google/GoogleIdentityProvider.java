package eu.ciechanowiec.sling.rocket.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.services.directory.Directory;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.apache.jackrabbit.oak.spi.security.authentication.external.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import java.util.Iterator;
import java.util.Optional;

/**
 * {@link ExternalIdentityProvider} based on Google {@link Directory}.
 */
@Component(
    service = {ExternalIdentityProvider.class, GoogleIdentityProvider.class, GoogleIdentityProviderMBean.class},
    immediate = true,
    property = "jmx.objectname=eu.ciechanowiec.slexamplus:type=Identity Management,name=Google Identity Provider"
)
@SuppressWarnings({"TypeName", "NullableProblems", "PMD.LooseCoupling"})
@ServiceDescription(GoogleIdentityProvider.SERVICE_DESCRIPTION)
@ToString
@Slf4j
public class GoogleIdentityProvider extends AnnotatedStandardMBean
    implements ExternalIdentityProvider, GoogleIdentityProviderMBean {

    static final String SERVICE_DESCRIPTION = "External Identity Provider based on Google Directory";
    private final GoogleDirectory googleDirectory;
    private final GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy;

    /**
     * Constructs an instance of this class.
     *
     * @param googleDirectory {@link GoogleDirectory} used by the constructed instance
     * @param googleIdTokenVerifierProxy {@link GoogleIdTokenVerifierProxy} used by the constructed instance
     */
    @Activate
    public GoogleIdentityProvider(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        GoogleDirectory googleDirectory,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        GoogleIdTokenVerifierProxy googleIdTokenVerifierProxy
    ) {
        super(GoogleIdentityProviderMBean.class);
        this.googleDirectory = googleDirectory;
        this.googleIdTokenVerifierProxy = googleIdTokenVerifierProxy;
        log.info("{} initialized", this);
    }

    @Override
    public String getName() {
        return GoogleIdentityProvider.class.getSimpleName();
    }

    @Override
    public ExternalIdentity getIdentity(ExternalIdentityRef ref) {
        log.trace("getIdentity({})", ref);
        return Optional.ofNullable(ref.getProviderName())
            .filter(providerNameFromEIR -> providerNameFromEIR.equals(getName()))
            .map(providerNameFromEIR -> ref.getId())
            .flatMap(
                externalIdentityRefID -> googleDirectory.retrieveUser(externalIdentityRefID)
                    .<ExternalIdentity>map(user -> new GoogleExternalUser(user, googleDirectory))
                    .or(
                        () -> googleDirectory.retrieveGroup(externalIdentityRefID)
                            .map(group -> new GoogleExternalGroup(group, googleDirectory))
                    )
            ).orElse(null);
    }

    @Override
    public ExternalUser getUser(String userId) {
        log.trace("getUser({})", userId);
        return googleDirectory.retrieveUser(userId)
            .map(user -> new GoogleExternalUser(user, googleDirectory))
            .orElse(null);
    }

    @Override
    public ExternalUser authenticate(Credentials credentials) {
        log.trace("Authenticating {}", credentials);
        return Optional.of(credentials)
            .filter(SimpleCredentials.class::isInstance)
            .map(SimpleCredentials.class::cast)
            .map(GoogleSimpleCredentials::new)
            .flatMap(this::authenticate)
            .orElse(null);
    }

    private Optional<ExternalUser> authenticate(GoogleSimpleCredentials googleSimpleCredentials) {
        log.trace("Authenticating {}", googleSimpleCredentials);
        String actualEmail = googleSimpleCredentials.email();
        return extractPayload(googleSimpleCredentials).map(GoogleIdToken.Payload::getEmail)
            .filter(extractedEmail -> extractedEmail.equals(actualEmail))
            .map(this::getUser);
    }

    @Override
    public Optional<ExternalUser> authenticate(String email, String idToken) {
        return Optional.ofNullable(authenticate(new SimpleCredentials(email, idToken.toCharArray())));
    }

    private Optional<GoogleIdToken.Payload> extractPayload(
        GoogleSimpleCredentials googleSimpleCredentials
    ) {
        log.trace("Extracting payload from {}", googleSimpleCredentials);
        String actualIDToken = googleSimpleCredentials.idToken();
        return googleIdTokenVerifierProxy.verify(actualIDToken)
            .map(GoogleIdToken::getPayload)
            .map(
                payload -> {
                    log.trace("From {} this payload extracted: {}", googleSimpleCredentials, payload);
                    return payload;
                }
            );
    }

    @Override
    public ExternalGroup getGroup(String name) {
        log.trace("getGroup({})", name);
        return googleDirectory.retrieveGroup(name)
            .map(group -> new GoogleExternalGroup(group, googleDirectory))
            .orElse(null);
    }

    @Override
    public Iterator<ExternalUser> listUsers() {
        return googleDirectory.listUsers()
            .stream()
            .<ExternalUser>map(user -> new GoogleExternalUser(user, googleDirectory))
            .toList()
            .iterator();
    }

    @Override
    public Iterator<ExternalGroup> listGroups() {
        return googleDirectory.listGroups()
            .stream()
            .<ExternalGroup>map(group -> new GoogleExternalGroup(group, googleDirectory))
            .toList()
            .iterator();
    }
}
