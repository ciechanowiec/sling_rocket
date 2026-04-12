package eu.ciechanowiec.sling.rocket.observation.stats.consistency;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.observation.audit.Entry;
import eu.ciechanowiec.sling.rocket.observation.audit.EntryTrampoline;
import eu.ciechanowiec.sling.rocket.observation.stats.RocketStats;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.ResourceResolver;
import org.jspecify.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import javax.jcr.Repository;
import javax.jcr.Session;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reports on the consistency of the {@link Repository}.
 */
@SuppressWarnings("ClassFanOutComplexity")
@Component(
    service = {JCRConsistencyStats.class, RocketStats.class},
    immediate = true,
    configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@Designate(ocd = JCRConsistencyStatsConfig.class)
@Slf4j
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE
)
@ServiceDescription("Reports on the consistency of the repository")
@ToString(onlyExplicitlyIncluded = true)
public class JCRConsistencyStats implements RocketStats {

    private final FullResourceAccess fullResourceAccess;
    private final BundleContext bundleContext;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<EntryTrampoline> entryTrampoline;
    @ToString.Include
    private final AtomicReference<JCRConsistencyStatsConfig> config;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} to acquire {@link ResourceResolver} for accessing the
     *                           {@link Repository} and obtaining the {@link Repository} version
     * @param bundleContext      {@link BundleContext} to obtain the {@link Repository} home path
     * @param entryTrampoline    {@link EntryTrampoline} to log the results of the consistency check
     * @param config             {@link JCRConsistencyStatsConfig} used by the constructed instance
     *
     */
    @Activate
    public JCRConsistencyStats(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess,
        BundleContext bundleContext,
        @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY
        )
        @Nullable
        EntryTrampoline entryTrampoline,
        JCRConsistencyStatsConfig config
    ) {
        this.fullResourceAccess = fullResourceAccess;
        this.bundleContext = bundleContext;
        this.entryTrampoline = Optional.ofNullable(entryTrampoline);
        this.config = new AtomicReference<>(config);
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(JCRConsistencyStatsConfig config) {
        this.config.set(config);
        log.info("Reconfigured: {}", this);
    }

    private Optional<String> lastValidSegment(Path segmentStoreAbsPath) {
        Pattern goodRevisionPattern = Pattern.compile(
            "Latest good revision for paths and checkpoints checked is "
                + "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?::.*?)?)"
                + " from"
        );
        return runCheck(segmentStoreAbsPath).stream()
            .flatMap(String::lines)
            .map(goodRevisionPattern::matcher)
            .filter(Matcher::find)
            .map(matcher -> matcher.group(1))
            .reduce((_, second) -> second);
    }

    @SuppressWarnings("MagicNumber")
    private Optional<List<String>> journalLogRecentSegments(Path segmentStoreAbsPath) {
        return journalLogAbsPath(segmentStoreAbsPath).flatMap(
            path -> {
                try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
                    List<String> recentSegments = lines.map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .map(line -> line.split("\\s+")[0])
                        .toList();
                    int size = recentSegments.size();
                    return Optional.of(recentSegments.subList(Math.max(size - 15, 0), size));
                } catch (IOException exception) {
                    log.error("Unable to read the journal.log for {}", segmentStoreAbsPath, exception);
                    return Optional.empty();
                }
            }
        );
    }

    @JsonProperty
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private JSON nativeSegmentStore() {
        return segmentStoreAbsPathRepositoryHome().map(
                path -> new JCRConsistencyInfo(
                    path,
                    () -> lastValidSegment(path),
                    () -> journalLogRecentSegments(path)
                )
            ).map(JSON.class::cast)
            .orElse(new JCRConsistencyUnavailable());
    }

    @JsonProperty
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private JSON backupSegmentStore() {
        return Optional.of(config)
            .map(AtomicReference::get)
            .map(JCRConsistencyStatsConfig::backup_segmentstore_path)
            .map(Path::of)
            .filter(Files::isDirectory)
            .map(
                path -> new JCRConsistencyInfo(
                    path,
                    () -> lastValidSegment(path),
                    () -> journalLogRecentSegments(path)
                )
            ).map(JSON.class::cast)
            .orElse(new JCRConsistencyUnavailable());
    }

    private Optional<Path> journalLogAbsPath(Path segmentStoreAbsPath) {
        return Optional.of(segmentStoreAbsPath).map(
                Path::toString
            ).map("%s/journal.log"::formatted)
            .map(Path::of)
            .filter(Files::isRegularFile)
            .filter(Files::exists);
    }

    private Optional<String> runCheck(Path segmentStoreAbsPath) {
        return oakRunAbsPath().flatMap(oakRunAbsPath -> runCheck(oakRunAbsPath, segmentStoreAbsPath));
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints", "ReturnCount"})
    private Optional<String> runCheck(Path oakRunAbsPath, Path segmentStoreAbsPath) {
        log.debug("Running the check for {} with {}", segmentStoreAbsPath, oakRunAbsPath);
        ProcessBuilder processBuilder = new ProcessBuilder(
            "java",
            "-jar",
            oakRunAbsPath.toString(),
            "check",
            segmentStoreAbsPath.toString(),
            "--bin"
        );
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String checkResult = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            log.debug("Check ended with exit code {} and result {}", exitCode, checkResult);
            entryTrampoline.ifPresent(
                entryTrampolineNonNull ->
                    entryTrampolineNonNull.submitForSaving(
                        new Entry(
                            "system", JCRConsistencyStats.class.getName(), LocalDateTime.now(),
                            Map.of(
                                "segmentStoreAbsPath", segmentStoreAbsPath.toString(),
                                "oakRunAbsPath", oakRunAbsPath.toString(),
                                "exitCode", String.valueOf(exitCode),
                                "checkResult", checkResult
                            )
                        )
                    )
            );
            return Optional.of(checkResult);
        } catch (IOException exception) {
            log.error("Unable to run the check for {} with {}", segmentStoreAbsPath, oakRunAbsPath, exception);
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Unable to run the check for {} with {}", segmentStoreAbsPath, oakRunAbsPath, exception);
            return Optional.empty();
        }
    }

    private Optional<Path> oakRunAbsPath() {
        return slingDirAbsPath().flatMap(
            slingDir -> jcrRepositoryVersion().map(
                    jcrRepositoryVersion -> "%s/oak-run-%s.jar".formatted(slingDir.toString(), jcrRepositoryVersion)
                ).map(Path::of)
                .filter(Files::isRegularFile)
                .filter(Files::exists));
    }

    private Optional<Path> segmentStoreAbsPathRepositoryHome() {
        return slingDirAbsPath().map(
            slingDir -> Optional.ofNullable(bundleContext.getProperty("repository.home"))
                .map("%s/segmentstore"::formatted)
                .orElse("%s/launcher/repository/segmentstore".formatted(slingDir.toString()))
        ).map(Path::of).filter(Files::isDirectory);
    }

    @SuppressWarnings("CallToSystemGetenv")
    private Optional<Path> slingDirAbsPath() {
        String slingDir = Optional.ofNullable(System.getenv("SLING_DIR"))
            .orElse("/opt/sling");
        return Optional.of(slingDir)
            .map(Path::of)
            .filter(Files::isDirectory);
    }

    private Optional<String> jcrRepositoryVersion() {
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.adaptTo(Session.class))
                .map(Session::getRepository)
                .map(repository -> repository.getDescriptor(Repository.REP_VERSION_DESC));
        }
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        return objectMapper.writeValueAsString(this);
    }

    @Override
    public String name() {
        return JCRConsistencyStats.class.getName();
    }
}
