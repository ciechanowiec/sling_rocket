package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Statistics on the external IP address of the system.
 */
@Component(
    service = {ExternalIPStats.class, RocketStats.class},
    immediate = true
)
@Slf4j
@ToString
@ServiceDescription("Statistics on the external IP address of the system")
public class ExternalIPStats implements RocketStats {

    private final List<URI> serviceURIs;

    /**
     * Constructs an instance of this class.
     */
    @Activate
    public ExternalIPStats() {
        this(
            Stream.of(
                    "https://checkip.amazonaws.com",
                    "https://api.ipify.org",
                    "https://icanhazip.com",
                    "https://ifconfig.me/ip"
                ).map(URI::create)
                .toList()
        );
    }

    ExternalIPStats(List<URI> serviceURIs) {
        this.serviceURIs = Collections.unmodifiableList(serviceURIs);
        log.info("Initialized with {}", serviceURIs);
    }

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<String> externalIP() {
        try (
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()
        ) {
            return serviceURIs.stream()
                .map(serviceURI -> externalIP(client, serviceURI))
                .flatMap(Optional::stream)
                .findFirst();
        }
    }

    private Optional<String> externalIP(HttpClient client, URI serviceURI) {
        Optional<String> result = Optional.empty();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(serviceURI)
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HttpServletResponse.SC_OK) {
                String externalIP = response.body().trim();
                log.trace("Received external IP from {}: {}", serviceURI, externalIP);
                result = Optional.of(externalIP);
            } else {
                log.warn("Non-OK response from {}: {}", serviceURI, response.statusCode());
            }
        } catch (IOException exception) {
            log.warn("Failed to get IP from {}", serviceURI, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Failed to get IP from {}", serviceURI, exception);
        }
        return result;
    }

    @Override
    public String name() {
        return ExternalIPStats.class.getName();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module());
        return mapper.writeValueAsString(this);
    }
}
