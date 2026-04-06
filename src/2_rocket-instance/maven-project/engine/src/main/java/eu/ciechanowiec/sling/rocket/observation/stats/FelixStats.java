package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Selected <a href="https://felix.apache.org/documentation/subprojects/apache-felix-healthchecks.html">Felix Health
 * Checks</a> delivered as {@link RocketStats}.
 * <p>
 * The following checks are included:
 * <ol>
 *     <li>{@link org.apache.felix.hc.generalchecks.BundlesStartedCheck}</li>
 *     <li>{@link org.apache.felix.hc.generalchecks.CpuCheck}</li>
 *     <li>{@link org.apache.felix.hc.generalchecks.MemoryCheck}</li>
 * </ol>
 */
@Component(
    service = {FelixStats.class, RocketStats.class},
    immediate = true
)
@ServiceDescription("Selected Felix Health Checks delivered as RocketStats")
public class FelixStats implements RocketStats {

    private static final String BUNDLES_STARTED_CHECK = "org.apache.felix.hc.generalchecks.BundlesStartedCheck";
    private static final String CPU_CHECK = "org.apache.felix.hc.generalchecks.CpuCheck";
    private static final String MEMORY_CHECK = "org.apache.felix.hc.generalchecks.MemoryCheck";

    private final List<HealthCheck> checks;

    /**
     * Constructs an instance of this class.
     *
     * @param bundlesStartedCheck {@link HealthCheck} of type
     *                            {@link org.apache.felix.hc.generalchecks.BundlesStartedCheck}
     * @param cpuCheck            {@link HealthCheck} of type {@link org.apache.felix.hc.generalchecks.CpuCheck}
     * @param memoryCheck         {@link HealthCheck} of type {@link org.apache.felix.hc.generalchecks.MemoryCheck}
     */
    @Activate
    public FelixStats(
        @Reference(
            cardinality = ReferenceCardinality.MANDATORY,
            target = "(" + ComponentConstants.COMPONENT_NAME + "=" + BUNDLES_STARTED_CHECK + ")"
        )
        HealthCheck bundlesStartedCheck,
        @Reference(
            cardinality = ReferenceCardinality.MANDATORY,
            target = "(" + ComponentConstants.COMPONENT_NAME + "=" + CPU_CHECK + ")"
        )
        HealthCheck cpuCheck,
        @Reference(
            cardinality = ReferenceCardinality.MANDATORY,
            target = "(" + ComponentConstants.COMPONENT_NAME + "=" + MEMORY_CHECK + ")"
        )
        HealthCheck memoryCheck
    ) {
        checks = List.of(bundlesStartedCheck, cpuCheck, memoryCheck);
    }

    @JsonValue
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private Map<String, String> hcWithStatuses() {
        return checks.stream()
            .collect(
                Collectors.toMap(
                    check -> check.getClass().getName(),
                    this::status
                )
            );
    }

    private String status(HealthCheck healthCheck) {
        Result result = healthCheck.execute();
        Result.Status resultStatus = result.getStatus();
        String resultLastMessage = Optional.of(IteratorUtils.toList(result.iterator()))
            .filter(resultEntries -> !resultEntries.isEmpty())
            .map(List::getLast)
            .map(ResultLog.Entry::getMessage)
            .orElse(StringUtils.EMPTY);
        return "[%s] %s".formatted(resultStatus, resultLastMessage);
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }

    @Override
    public String name() {
        return this.getClass().getName();
    }
}
