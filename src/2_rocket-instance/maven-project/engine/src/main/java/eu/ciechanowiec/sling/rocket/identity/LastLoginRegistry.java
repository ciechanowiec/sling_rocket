package eu.ciechanowiec.sling.rocket.identity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.ciechanowiec.sling.rocket.observation.stats.RocketStats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.JakartaAuthenticationInfoPostProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registry of last login facts for {@link User}s.
 */
@Component(
    service = {
        LastLoginRegistry.class, EventHandler.class, JakartaAuthenticationInfoPostProcessor.class,
        RocketStats.class
    },
    immediate = true,
    property = EventConstants.EVENT_TOPIC + "=" + AuthConstants.TOPIC_LOGIN
)
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE
)
@ServiceDescription("Registry of last login facts for users")
public class LastLoginRegistry implements JakartaAuthenticationInfoPostProcessor, RocketStats, EventHandler {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @JsonProperty
    private final Map<AuthIDUser, LocalDateTime> lastLoginAttempts;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @JsonProperty
    private final Map<AuthIDUser, LocalDateTime> lastAndFreshLoginSuccesses;

    @SuppressWarnings("unused")
    @JsonProperty
    private final String note;

    @SuppressWarnings("unused")
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private final LocalDateTime since;

    /**
     * Constructs an instance of this class.
     */
    @Activate
    public LastLoginRegistry() {
        lastLoginAttempts = new TreeMap<>();
        lastAndFreshLoginSuccesses = new TreeMap<>();
        since = LocalDateTime.now();
        note
            = "Due to session caching, the recorded last login attempt might be more recent than the recorded last "
            + "and fresh login success for the same user even if the attempt was successful";
    }

    @Override
    public void postProcess(
        AuthenticationInfo info, HttpServletRequest request,
        HttpServletResponse response
    ) {
        Optional.ofNullable(info.getUser())
            .map(AuthIDUser::new)
            .ifPresent(this::updateLastLoginTime);
    }

    private void updateLastLoginTime(AuthIDUser user) {
        lastLoginAttempts.put(user, LocalDateTime.now());
    }

    @Override
    public String name() {
        return LastLoginRegistry.class.getName();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper.writeValueAsString(this);
    }

    @Override
    public void handleEvent(Event event) {
        Optional.of(event.getTopic())
            .filter(AuthConstants.TOPIC_LOGIN::equals)
            .map(_ -> event.getProperty(SlingConstants.PROPERTY_USERID))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(AuthIDUser::new)
            .ifPresent(authIDUser -> lastAndFreshLoginSuccesses.put(authIDUser, LocalDateTime.now()));
    }
}
