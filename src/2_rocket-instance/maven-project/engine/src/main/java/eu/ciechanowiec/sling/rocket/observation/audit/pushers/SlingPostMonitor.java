package eu.ciechanowiec.sling.rocket.observation.audit.pushers;

import eu.ciechanowiec.sling.rocket.observation.audit.Entry;
import eu.ciechanowiec.sling.rocket.observation.audit.EntryTrampoline;
import jakarta.servlet.http.HttpServletRequest;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingJakartaPostProcessor;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    "Sling POST Monitor that converts the incoming modifications into entries and submits them to the EntryTrampoline"
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
        String userID = Optional.ofNullable(request.getRemoteUser()).orElse(Entry.UNKNOWN);
        ModificationType modificationType = modification.getType();
        String source = Optional.ofNullable(modification.getSource()).orElse(Entry.UNKNOWN);
        String destination = Optional.ofNullable(modification.getDestination()).orElse(Entry.UNKNOWN);
        String threadName = Thread.currentThread().getName();
        Entry entry = new Entry(
            userID,
            Modification.class.getName(),
            LocalDateTime.now(),
            Map.of(
                ModificationType.class.getName(), modificationType.name(),
                "source", source,
                "destination", destination,
                "threadName", threadName
            )
        );
        entryTrampoline.submitForSaving(entry);
    }
}
