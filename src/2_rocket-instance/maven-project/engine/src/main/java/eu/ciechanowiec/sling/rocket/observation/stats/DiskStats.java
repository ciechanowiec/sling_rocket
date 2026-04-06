package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Statistics on the disk usage of the system.
 */
@Component(
    service = {RocketStats.class, DiskStats.class},
    immediate = true
)
@ServiceDescription("Statistics on the disk usage of the system")
@Slf4j
@ToString
public class DiskStats implements RocketStats {

    /**
     * Constructs an instance of this class.
     */
    @Activate
    public DiskStats() {
        log.info("Initialized {}", this);
    }

    @SneakyThrows
    @JsonValue
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    DiskCalculation calculate() {
        log.info("Collecting disk stats");
        Path rootPath = Paths.get("/");
        FileStore fileStore = Files.getFileStore(rootPath);
        long totalSpace = fileStore.getTotalSpace();
        long usableSpace = fileStore.getUsableSpace();
        long occupiedSpace = totalSpace - usableSpace;
        return new DiskCalculation(
            new DataSize(totalSpace, DataUnit.BYTES),
            new DataSize(occupiedSpace, DataUnit.BYTES),
            new DataSize(usableSpace, DataUnit.BYTES)
        );
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    @Override
    public String name() {
        return DiskStats.class.getName();
    }
}
