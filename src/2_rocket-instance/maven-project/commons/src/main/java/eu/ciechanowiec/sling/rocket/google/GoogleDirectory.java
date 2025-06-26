package eu.ciechanowiec.sling.rocket.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Google {@link Directory}.
 */
@Component(
    service = {GoogleDirectory.class, GoogleDirectoryMBean.class},
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = "jmx.objectname=eu.ciechanowiec.slexamplus:type=Identity Management,name=Google Directory"
)
@Designate(ocd = GoogleDirectoryConfig.class)
@ToString
@Slf4j
@ServiceDescription(GoogleDirectory.SERVICE_DESCRIPTION)
@SuppressWarnings("PMD.LooseCoupling")
public class GoogleDirectory extends AnnotatedStandardMBean implements GoogleDirectoryMBean {

    static final String SERVICE_DESCRIPTION = "Google Directory";
    private final AtomicReference<GoogleDirectoryConfig> config;

    /**
     * Constructs an instance of this class.
     *
     * @param config {@link GoogleDirectoryConfig} used by the constructed instance
     */
    @Activate
    public GoogleDirectory(GoogleDirectoryConfig config) {
        super(GoogleDirectoryMBean.class);
        this.config = new AtomicReference<>(config);
        log.info("{} initialized", this);
    }

    @Modified
    void configure(GoogleDirectoryConfig config) {
        log.debug("Configuring {}", this);
        this.config.set(config);
        log.debug("Configured {}", this);
    }

    private Directory buildDirectory() throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        String pathToServiceAccountKeyFile = config.get().path$_$to$_$service$_$account$_$key$_$file();
        try (InputStream credentialsFIS = Files.newInputStream(Paths.get(pathToServiceAccountKeyFile))) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsFIS)
                .createScoped(List.of(config.get().directory$_$scopes()))
                .createDelegated(config.get().user$_$to$_$impersonate_email());
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
            return new Directory.Builder(httpTransport, GsonFactory.getDefaultInstance(), requestInitializer).build();
        }
    }

    @Override
    public List<User> listUsers() {
        log.trace("Listing users");
        try {
            List<User> users = buildDirectory().users().list().setCustomer("my_customer").execute().getUsers();
            log.trace("Found {} users", users.size());
            return users;
        } catch (IOException | GeneralSecurityException exception) {
            log.error("Unable to list users", exception);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Group> listGroups() {
        log.trace("Listing groups");
        try {
            List<Group> groups = buildDirectory().groups().list().setCustomer("my_customer").execute().getGroups();
            log.trace("Found {} groups", groups.size());
            return groups;
        } catch (IOException | GeneralSecurityException exception) {
            log.error("Unable to list groups", exception);
            return Collections.emptyList();
        }
    }

    @Override
    @SuppressWarnings("MethodCallInLoopCondition")
    public List<Group> listGroups(String memberKey) {
        log.trace("Listing groups for '{}'", memberKey);
        List<Group> allGroups = new ArrayList<>();
        try {
            Directory.Groups.List request = buildDirectory().groups().list().setUserKey(memberKey);
            String pageToken = null;
            do {
                request.setPageToken(pageToken);
                Groups result = request.execute();
                Optional.ofNullable(result.getGroups())
                    .ifPresent(allGroups::addAll);
                pageToken = result.getNextPageToken();
            } while (Objects.nonNull(pageToken));
            log.trace("Retrieved {} groups for '{}'", allGroups.size(), memberKey);
            return Collections.unmodifiableList(allGroups);
        } catch (IOException | GeneralSecurityException exception) {
            String message = "Unable to list groups for '%s'".formatted(memberKey);
            log.warn(message, exception);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<User> retrieveUser(String userKey) {
        log.trace("Retrieving the user '{}'", userKey);
        try {
            User user = buildDirectory().users().get(userKey).execute();
            log.trace("Retrieved '{}'", user);
            return Optional.of(user);
        } catch (IOException | GeneralSecurityException exception) {
            String message = "User '%s' not found".formatted(userKey);
            log.debug(message, exception);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Group> retrieveGroup(String groupKey) {
        log.trace("Retrieving the group '{}'", groupKey);
        try {
            Group group = buildDirectory().groups().get(groupKey).execute();
            log.trace("Retrieved '{}'", group);
            return Optional.of(group);
        } catch (IOException | GeneralSecurityException exception) {
            String message = "Group '%s' not found".formatted(groupKey);
            log.debug(message, exception);
            return Optional.empty();
        }
    }

    @SuppressWarnings("MethodCallInLoopCondition")
    @Override
    public List<Member> listMembers(String groupKey) {
        log.trace("Listing members for group '{}'", groupKey);
        List<Member> allMembers = new ArrayList<>();
        try {
            Directory.Members.List request = buildDirectory().members().list(groupKey);
            String pageToken;
            do {
                Members members = request.execute();
                List<Member> currentPage = members.getMembers();
                Optional.ofNullable(currentPage)
                    .ifPresent(allMembers::addAll);
                pageToken = members.getNextPageToken();
                request.setPageToken(pageToken);
            } while (Objects.nonNull(pageToken));
            log.trace("Retrieved {} members for group '{}'", allMembers.size(), groupKey);
            return Collections.unmodifiableList(allMembers);
        } catch (IOException | GeneralSecurityException exception) {
            String message = "Unable to list members for group '%s'".formatted(groupKey);
            log.warn(message, exception);
            return Collections.emptyList();
        }
    }
}
