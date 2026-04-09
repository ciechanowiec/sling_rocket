package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * {@link RocketStats} that provides information on the {@link SlingSettingsService#getRunModes()} of the current
 * application instance.
 */
@Component(
    service = {RunModesStats.class, RocketStats.class},
    immediate = true
)
@ServiceDescription("RocketStats that provides information on the run modes of the current application instance")
public class RunModesStats implements RocketStats {

    private final SlingSettingsService slingSettingsService;

    /**
     * Constructs an instance of this class.
     *
     * @param slingSettingsService {@link SlingSettingsService} to be used by this class to obtain the run modes of the
     *                             current application instance
     */
    @Activate
    public RunModesStats(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        SlingSettingsService slingSettingsService
    ) {
        this.slingSettingsService = slingSettingsService;
    }

    @JsonProperty
    SortedSet<String> runModes() {
        return new TreeSet<>(slingSettingsService.getRunModes());
    }

    @Override
    public String name() {
        return RunModesStats.class.getName();
    }

    @Override
    @SneakyThrows
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
