package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Native Sling Rocket statistics.
 */
@Component(
    service = {RocketStats.class, NativeStats.class},
    immediate = true
)
@ServiceDescription("Native Sling Rocket statistics")
@Slf4j
@SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
public class NativeStats implements RocketStats {

    /**
     * {@link FullResourceAccess} that will be used by this object to acquire access to resources.
     */
    private final FullResourceAccess fullResourceAccess;

    private final Cache<String, DiskStats> diskStatsCache;

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
        this.diskStatsCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    }

    @SneakyThrows
    @JsonProperty("diskStats")
    DiskStats diskStats() {
        return diskStatsCache.get(
            "diskStats", SneakyFunction.sneaky(
                _ -> {
                    log.info("Collecting disk stats");
                    Path rootPath = Paths.get("/");
                    FileStore fileStore = Files.getFileStore(rootPath);
                    long totalSpace = fileStore.getTotalSpace();
                    long usableSpace = fileStore.getUsableSpace();
                    long occupiedSpace = totalSpace - usableSpace;
                    return new DiskStats(
                        new DataSize(totalSpace, DataUnit.BYTES),
                        new DataSize(occupiedSpace, DataUnit.BYTES),
                        new DataSize(usableSpace, DataUnit.BYTES),
                        Instant.now(),
                        "1 minute"
                    );
                }
            )
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
