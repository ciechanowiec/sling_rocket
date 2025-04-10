package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import java.io.File;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

/**
 * Native Sling Rocket statistics.
 */
@Component(
    service = {RocketStats.class, NativeStats.class},
    immediate = true
)
@ServiceDescription("Native Sling Rocket statistics")
@Slf4j
public class NativeStats implements RocketStats {

    /**
     * {@link FullResourceAccess} that will be used by this object to acquire access to resources.
     */
    private final FullResourceAccess fullResourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                           access to resources
     */
    @Activate
    public NativeStats(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess
    ) {
        this.fullResourceAccess = fullResourceAccess;
    }

    @JsonProperty("diskStats")
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    DiskStats diskStats() {
        log.info("Collecting disk stats");
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long occupiedSpace = totalSpace - freeSpace;
        return new DiskStats(
            new DataSize(totalSpace, DataUnit.BYTES),
            new DataSize(occupiedSpace, DataUnit.BYTES),
            new DataSize(freeSpace, DataUnit.BYTES)
        );
    }

    @JsonProperty("assetsStats")
    AssetsStats assetsStats() {
        log.info("Collecting Assets stats");
        return new AssetsStats(fullResourceAccess);
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(this);
    }

    @Override
    public String name() {
        return NativeStats.class.getName();
    }
}
