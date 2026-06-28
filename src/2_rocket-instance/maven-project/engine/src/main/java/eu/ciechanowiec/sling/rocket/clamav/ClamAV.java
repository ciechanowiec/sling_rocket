package eu.ciechanowiec.sling.rocket.clamav;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetFile;
import eu.ciechanowiec.sling.rocket.observation.stats.RocketStats;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * {@link VirusScanner} that scans content with a ClamAV daemon (clamd) over the clamd TCP protocol.
 * <p>
 * By default, the ClamAV daemon (clamd) of the standard <i>Sling Rocket</i> deployment at {@code rocket-clamav:3310} is
 * used, so this {@link VirusScanner} is fully functional without any explicit {@link ClamAVConfig}.
 */
@Component(
    service = {VirusScanner.class, RocketStats.class, ClamAV.class},
    immediate = true,
    configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@Slf4j
@ServiceDescription("ClamAV antivirus scanner")
@Designate(ocd = ClamAVConfig.class)
@SuppressWarnings({"unused", "WeakerAccess"})
@ToString
public class ClamAV implements VirusScanner, RocketStats {

    private final AtomicReference<ClamAVConfig> config;
    private final LongAdder cleanScans;
    private final LongAdder infectedScans;
    private final LongAdder failedScans;

    @SuppressWarnings("unused")
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private final LocalDateTime since;

    /**
     * Constructs an instance of this class.
     *
     * @param config {@link ClamAVConfig} to configure this {@link ClamAV}
     */
    @Activate
    public ClamAV(ClamAVConfig config) {
        this.config = new AtomicReference<>(config);
        this.cleanScans = new LongAdder();
        this.infectedScans = new LongAdder();
        this.failedScans = new LongAdder();
        this.since = LocalDateTime.now();
        log.info("Initialized {}", this);
    }

    /**
     * Configures this {@link ClamAV}.
     *
     * @param config {@link ClamAVConfig} to configure this {@link ClamAV}
     */
    @Modified
    void configure(ClamAVConfig config) {
        this.config.set(config);
        log.info("Configured {}", this);
    }

    @Override
    public ScanResult scan(InputStream content) {
        return register(scanWithoutRegistering(content));
    }

    @Override
    public ScanResult scan(Asset asset) {
        log.trace("{} is scanning {}", this, asset);
        return scan(asset.assetFile());
    }

    @Override
    public ScanResult scan(AssetFile assetFile) {
        return register(scanWithoutRegistering(assetFile));
    }

    /**
     * Total number of {@link ScanResult}s produced by this {@link ClamAV}. The counter is kept in memory only and
     * resets when the application restarts.
     *
     * @return total number of {@link ScanResult}s produced by this {@link ClamAV}
     */
    @JsonProperty
    public long numOfScans() {
        return numOfCleanScans() + numOfInfectedScans() + numOfFailedScans();
    }

    /**
     * Number of {@link Clean} {@link ScanResult}s produced by this {@link ClamAV}. The counter is kept in memory only
     * and resets when the application restarts.
     *
     * @return number of {@link Clean} {@link ScanResult}s produced by this {@link ClamAV}
     */
    @JsonProperty
    public long numOfCleanScans() {
        return cleanScans.sum();
    }

    /**
     * Number of {@link Infected} {@link ScanResult}s produced by this {@link ClamAV}. The counter is kept in memory
     * only and resets when the application restarts.
     *
     * @return number of {@link Infected} {@link ScanResult}s produced by this {@link ClamAV}
     */
    @JsonProperty
    public long numOfInfectedScans() {
        return infectedScans.sum();
    }

    /**
     * Number of {@link Failed} {@link ScanResult}s produced by this {@link ClamAV}. The counter is kept in memory only
     * and resets when the application restarts.
     *
     * @return number of {@link Failed} {@link ScanResult}s produced by this {@link ClamAV}
     */
    @JsonProperty
    public long numOfFailedScans() {
        return failedScans.sum();
    }

    private ScanResult scanWithoutRegistering(AssetFile assetFile) {
        try (InputStream content = assetFile.retrieve()) {
            return scanWithoutRegistering(content);
        } catch (IOException exception) {
            log.warn("Unable to scan an asset file", exception);
            return new Failed("Unable to read the scanned content: " + exception);
        }
    }

    private ScanResult scanWithoutRegistering(InputStream content) {
        try (ClamdConnection connection = connect()) {
            return connection.scan(content);
        } catch (IOException exception) {
            log.warn("Unable to scan content", exception);
            return new Failed("clamd is unreachable: " + exception);
        }
    }

    private ScanResult register(ScanResult scanResult) {
        switch (scanResult) {
            case Clean _ -> cleanScans.increment();
            case Infected _ -> infectedScans.increment();
            case Failed _ -> failedScans.increment();
        }
        log.trace("{} produced this scan result: {}", this, scanResult.summary());
        return scanResult;
    }

    @JsonProperty("reachable")
    @Override
    public boolean ping() {
        try (ClamdConnection connection = connect()) {
            return connection.ping();
        } catch (IOException exception) {
            log.warn("Unable to ping clamd", exception);
            return false;
        }
    }

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @Override
    public Optional<String> version() {
        try (ClamdConnection connection = connect()) {
            return connection.version();
        } catch (IOException exception) {
            log.warn("Unable to retrieve the clamd version", exception);
            return Optional.empty();
        }
    }

    @Override
    public String name() {
        return ClamAV.class.getName();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper.writeValueAsString(this);
    }

    private ClamdConnection connect() throws IOException {
        ClamAVConfig clamAVConfig = config.get();
        return new ClamdConnection(
            clamAVConfig.clamav_host(),
            clamAVConfig.clamav_port(),
            clamAVConfig.clamav_connect$_$timeout(),
            clamAVConfig.clamav_read$_$timeout()
        );
    }
}
