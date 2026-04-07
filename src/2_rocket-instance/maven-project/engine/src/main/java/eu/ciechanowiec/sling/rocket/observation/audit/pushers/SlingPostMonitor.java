package eu.ciechanowiec.sling.rocket.observation.audit.pushers;

import eu.ciechanowiec.sling.rocket.observation.audit.Entry;
import eu.ciechanowiec.sling.rocket.observation.audit.EntryTrampoline;
import jakarta.servlet.http.HttpServletRequest;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingJakartaPostProcessor;
import org.eclipse.jetty.http.HttpHeader;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * {@link SlingJakartaPostProcessor} that converts the incoming {@link Modification}s into {@link Entry}-s and sumbits
 * them to the {@link EntryTrampoline}.
 */
@Component(
    service = {SlingJakartaPostProcessor.class, SlingPostMonitor.class},
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Slf4j
@Designate(
    ocd = SlingPostMonitorConfig.class
)
@ToString
@ServiceDescription(
    "Sling POST Processor that converts the incoming modifications into entries and submits them to the EntryTrampoline"
)
public class SlingPostMonitor implements SlingJakartaPostProcessor {

    private final AtomicReference<SlingPostMonitorConfig> config;
    private final EntryTrampoline entryTrampoline;

    /**
     * Constructs an instance of this class.
     *
     * @param entryTrampoline {@link EntryTrampoline} to which the constructed object will submit {@link Entry}-s
     *                        constructed based on the received {@link Modification}-s
     * @param config          {@link SlingPostMonitorConfig} used by the constructed instance
     */
    @Activate
    public SlingPostMonitor(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        EntryTrampoline entryTrampoline,
        SlingPostMonitorConfig config
    ) {
        this.config = new AtomicReference<>(config);
        this.entryTrampoline = entryTrampoline;
        log.info("Activated {}", this);
    }

    @Modified
    void configure(SlingPostMonitorConfig config) {
        this.config.set(config);
        log.info("Configured {}", this);
    }

    @Override
    public void process(SlingJakartaHttpServletRequest request, List<Modification> changes) {
        if (config.get().is$_$enabled()) {
            changes.forEach(change -> process(request, change));
        }
    }

    private void process(HttpServletRequest request, Modification modification) {
        log.trace("Processing {}", modification);
        Map<String, String> additionalProperties = new ConcurrentHashMap<>();
        String userID = Optional.ofNullable(request.getRemoteUser()).orElse(Entry.UNKNOWN);
        ModificationType modificationType = modification.getType();
        String source = Optional.ofNullable(modification.getSource()).orElse(Entry.UNKNOWN);
        String destination = Optional.ofNullable(modification.getDestination()).orElse(Entry.UNKNOWN);
        String threadName = Thread.currentThread().getName();
        additionalProperties.put(ModificationType.class.getName(), modificationType.name());
        additionalProperties.put("source", source);
        additionalProperties.put("destination", destination);
        additionalProperties.put("threadName", threadName);
        additionalProperties.putAll(clientHeaders(request));
        Entry entry = new Entry(
            userID,
            Modification.class.getName(),
            LocalDateTime.now(),
            Collections.unmodifiableMap(additionalProperties)
        );
        entryTrampoline.submitForSaving(entry);
    }

    @SuppressWarnings({"squid:S3599", "squid:S1171"})
    private Map<String, String> clientHeaders(HttpServletRequest request) {
        Map<String, String> forwardedForHeaders = clientHeaders(request, HttpHeader.X_FORWARDED_FOR.name());
        Map<String, String> realIPHeaders = clientHeaders(request, "X-Real-IP");
        return Collections.unmodifiableMap(
            new ConcurrentHashMap<>() {{
                putAll(forwardedForHeaders);
                putAll(realIPHeaders);
            }}
        );
    }

    private Map<String, String> clientHeaders(HttpServletRequest request, String headerName) {
        List<String> headers = Optional.ofNullable(request.getHeaders(headerName))
            .map(enumeration -> (List<String>) Collections.list(enumeration))
            .orElse(List.of());
        int numOfHeaders = headers.size();
        boolean isSingularHeader = numOfHeaders == NumberUtils.INTEGER_ONE;
        Map<Boolean, Supplier<Map<String, String>>> strategies = Map.of(
            true, () -> Map.of(headerName, headers.getFirst()),
            false, () -> IntStream.range(0, numOfHeaders)
                .boxed()
                .collect(
                    Collectors.toMap(
                        index -> headerName + "[" + (index + 1) + "]",
                        headers::get,
                        (_, replacement) -> replacement,
                        ConcurrentHashMap::new
                    )
                )
        );
        return Collections.unmodifiableMap(strategies.get(isSingularHeader).get());
    }
}
