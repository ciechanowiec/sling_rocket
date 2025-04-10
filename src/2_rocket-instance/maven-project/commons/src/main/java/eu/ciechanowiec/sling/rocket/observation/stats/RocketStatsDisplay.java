package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.propertytypes.ServiceDescription;

/**
 * Display of Rocket Stats.
 */
@Component(
    service = {InventoryPrinter.class, RocketStatsDisplay.class},
    immediate = true,
    property = {
        InventoryPrinter.FORMAT + "=JSON",
        InventoryPrinter.FORMAT + "=TEXT",
        InventoryPrinter.TITLE + "=Sling Rocket Statistics",
        InventoryPrinter.NAME + "=sling-rocket-stats",
        InventoryPrinter.WEBCONSOLE + ":Boolean=true"
    }
)
@Slf4j
@ServiceDescription("Display of Rocket Stats")
public class RocketStatsDisplay implements InventoryPrinter, JSON {

    /**
     * {@link Collection} of {@link RocketStats} that will be displayed.
     */
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    @JsonSerialize(contentUsing = RocketStatsSerializer.class)
    @JsonProperty("rocketStats")
    private final Collection<RocketStats> rocketStats;

    /**
     * Constructs an instance of this class.
     */
    @Activate
    public RocketStatsDisplay() {
        rocketStats = new ArrayList<>();
    }

    @SneakyThrows
    @Override
    public void print(PrintWriter printWriter, Format format, boolean isZip) {
        log.debug("Requested stats");
        printWriter.write(asJSON());
    }

    @JsonProperty("generationTime")
    String now() {
        LocalDateTime now = LocalDateTime.now();
        Instant instant = now.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toString();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(this);
    }
}
