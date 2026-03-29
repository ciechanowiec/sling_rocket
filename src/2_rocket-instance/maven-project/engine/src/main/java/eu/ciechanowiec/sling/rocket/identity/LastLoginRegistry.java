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
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.JakartaAuthenticationInfoPostProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Component(
    service = {LastLoginRegistry.class, JakartaAuthenticationInfoPostProcessor.class, RocketStats.class},
    immediate = true
)
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE
)
public class LastLoginRegistry implements JakartaAuthenticationInfoPostProcessor, RocketStats {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @JsonProperty
    private final Map<AuthIDUser, LocalDateTime> lastLoginTimes;

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private final LocalDateTime since;

    @Activate
    public LastLoginRegistry() {
        lastLoginTimes = new TreeMap<>();
        since = LocalDateTime.now();
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
        lastLoginTimes.put(user, LocalDateTime.now());
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
}
