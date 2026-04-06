package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.management.JMException;
import javax.management.JMRuntimeException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Selected JMX statistics delivered as {@link RocketStats}.
 */
@Component(
    service = {JMXStats.class, RocketStats.class},
    immediate = true
)
@SuppressWarnings(
    {
        "squid:S1192", "MultipleStringLiterals", "PMD.AvoidDuplicateLiterals", "PMD.UnusedPrivateMethod",
        "PMD.UseConcurrentHashMap"
    }
)
@SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
@Slf4j
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE
)
@ServiceDescription("Selected JMX statistics delivered as RocketStats")
public class JMXStats implements JSON, RocketStats {

    /**
     * Constructs an instance of this class.
     */
    @Activate
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public JMXStats() {
        // Required by javadoc
    }

    private Map<String, Map<String, Object>> mbeanNamesToAttributes(
        Map<String, List<String>> mbeanNamesToAttributeNames
    ) {
        return mbeanNamesToAttributeNames.entrySet()
            .stream()
            .map(
                mbeanNameToAttributeNames -> {
                    String mbeanName = mbeanNameToAttributeNames.getKey();
                    List<String> attributeNames = mbeanNameToAttributeNames.getValue();
                    return mbeanNameToAttributes(mbeanName, attributeNames);
                }
            ).collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (v1, _) -> v1,
                    LinkedHashMap::new
                )
            );
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private Map.Entry<String, Map<String, Object>> mbeanNameToAttributes(
        String mbeanName, List<String> attributeNames
    ) {
        Map<String, Object> attributes = attributeNames.stream()
            .map(attributeName -> mbeanNameToAttribute(mbeanName, attributeName))
            .map(Map.Entry::getValue)
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (v1, _) -> v1,
                    LinkedHashMap::new
                )
            );
        return new AbstractMap.SimpleEntry<>(mbeanName, attributes);
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    @SneakyThrows
    private Map.Entry<String, Map.Entry<String, Object>> mbeanNameToAttribute(String mbeanName, String attributeName) {
        try {
            MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName = new ObjectName(mbeanName);
            Object attribute = mbeanServer.getAttribute(objectName, attributeName);
            return new AbstractMap.SimpleEntry<>(
                mbeanName, new AbstractMap.SimpleEntry<>(attributeName, attribute)
            );
        } catch (JMException | JMRuntimeException exception) {
            log.error("Error while acquiring attribute {} of MBean {}", attributeName, mbeanName, exception);
            String exceptionMessage = exception.getMessage();
            return new AbstractMap.SimpleEntry<>(
                mbeanName, new AbstractMap.SimpleEntry<>(attributeName, "N/A [%s]".formatted(exceptionMessage))
            );
        }
    }

    @JsonProperty("java.lang")
    private Map<String, Map<String, Object>> javaLang() {
        Map<String, List<String>> mbeanNamesToAttributeNames = new LinkedHashMap<>();
        mbeanNamesToAttributeNames.put(
            "java.lang:type=Threading", List.of("ThreadCount", "PeakThreadCount", "TotalStartedThreadCount")
        );
        mbeanNamesToAttributeNames.put("java.lang:type=Runtime", List.of("Name", "StartTime"));
        Map<String, Map<String, Object>> mbeanNamesToAttributes = mbeanNamesToAttributes(mbeanNamesToAttributeNames);
        Map<String, Map<String, Object>> resultAttributes = new LinkedHashMap<>(mbeanNamesToAttributes);

        Optional.ofNullable(resultAttributes.get("java.lang:type=Runtime")).ifPresent(
            runtimeAttributes -> {
                Map<String, Object> updatedRuntimeAttributes = new LinkedHashMap<>(runtimeAttributes);
                Optional.ofNullable(updatedRuntimeAttributes.get("StartTime"))
                    .filter(Number.class::isInstance)
                    .map(Number.class::cast)
                    .ifPresent(
                        number -> {
                            updatedRuntimeAttributes.put(
                                "StartTimeReadable",
                                Instant.ofEpochMilli(number.longValue()).toString()
                            );
                            long elapsedMillis = System.currentTimeMillis() - number.longValue();
                            updatedRuntimeAttributes.put("TimeElapsedFromStart", elapsedMillis);

                            Duration duration = Duration.ofMillis(elapsedMillis);
                            String readableElapsed = String.format(
                                "%d days, %d hours, %d minutes, %d seconds",
                                duration.toDaysPart(), duration.toHoursPart(), duration.toMinutesPart(),
                                duration.toSecondsPart()
                            );
                            updatedRuntimeAttributes.put("TimeElapsedFromStartReadable", readableElapsed);
                        }
                    );

                resultAttributes.put("java.lang:type=Runtime", updatedRuntimeAttributes);
            }
        );

        return Collections.unmodifiableMap(resultAttributes);
    }

    @JsonProperty("org.apache.jackrabbit.oak")
    private Map<String, Map<String, Object>> orgApacheJackrabbitOak() {
        Map<String, List<String>> mbeanNamesToAttributeNames = new LinkedHashMap<>();
        mbeanNamesToAttributeNames.put(
            "org.apache.jackrabbit.oak:name=FileStore statistics,type=FileStoreStats",
            List.of("SegmentCount", "ApproximateSize", "TarFileCount")
        );
        mbeanNamesToAttributeNames.put(
            "org.apache.jackrabbit.oak:name=NODE_COUNT_FROM_ROOT,type=Metrics", List.of("Count")
        );
        mbeanNamesToAttributeNames.put(
            "org.apache.jackrabbit.oak:name=SESSION_LOGIN_COUNTER,type=Metrics", List.of("Count")
        );
        mbeanNamesToAttributeNames.put(
            "org.apache.jackrabbit.oak:name=SESSION_WRITE_COUNTER,type=Metrics", List.of("Count")
        );
        mbeanNamesToAttributeNames.put("org.apache.jackrabbit.oak:name=SESSION_COUNT,type=Metrics", List.of("Count"));
        mbeanNamesToAttributeNames.put(
            "org.apache.jackrabbit.oak:name=COMMITS_COUNT,type=Metrics", List.of("Count")
        );
        mbeanNamesToAttributeNames.put(
            "org.apache.jackrabbit.oak:name=COMMIT_TIME,type=Metrics", List.of("Mean", "DurationUnit", "Count"));
        mbeanNamesToAttributeNames.put(
            "org.apache.jackrabbit.oak:name=QUERY_DURATION,type=Metrics", List.of("Mean", "DurationUnit", "Count")
        );
        mbeanNamesToAttributeNames.put(
            "org.apache.jackrabbit.oak:name=\"QUERY_DURATION;index=uuid\",type=Metrics",
            List.of("Mean", "DurationUnit", "Count")
        );
        return mbeanNamesToAttributes(mbeanNamesToAttributeNames);
    }

    @JsonProperty("org.apache.sling")
    private Map<String, Map<String, Object>> orgApacheSling() {
        Map<String, List<String>> mbeanNamesToAttributeNames = new LinkedHashMap<>();
        mbeanNamesToAttributeNames.put(
            "org.apache.sling:type=engine,service=RequestProcessor",
            List.of("RequestsCount", "MeanRequestDurationMsec")
        );
        mbeanNamesToAttributeNames.put(
            "org.apache.sling:name=AllQueues,type=queues",
            List.of(
                "StartDate", "LastFinishedJobDate", "NumberOfJobs", "NumberOfQueuedJobs", "NumberOfActiveJobs",
                "NumberOfFinishedJobs", "NumberOfFailedJobs", "NumberOfCancelledJobs", "NumberOfProcessedJobs",
                "AverageProcessingTime", "AverageWaitingTime"
            )
        );
        return mbeanNamesToAttributes(mbeanNamesToAttributeNames);
    }

    @JsonProperty("org.apache.sling.auth.core")
    private Map<String, Map<String, Object>> orgApacheSlingAuthCore() {
        Map<String, List<String>> mbeanNamesToAttributeNames = new LinkedHashMap<>();
        mbeanNamesToAttributeNames.put(
            "org.apache.sling.auth.core:name=sling.auth.core.authenticate.timer,type=Metrics",
            List.of(
                "Mean", "50thPercentile", "95thPercentile", "DurationUnit", "Count"
            )
        );
        mbeanNamesToAttributeNames.put(
            "org.apache.sling.auth.core:name=sling.auth.core.authenticate.failed,type=Metrics",
            List.of("Count")
        );
        return mbeanNamesToAttributes(mbeanNamesToAttributeNames);
    }

    @JsonProperty("org.apache.sling.resourceresolver")
    private Map<String, Map<String, Object>> orgApacheSlingResourceresolver() {
        Map<String, List<String>> mbeanNamesToAttributeNames = new LinkedHashMap<>();
        mbeanNamesToAttributeNames.put(
            "org.apache.sling.resourceresolver:"
                + "name=org.apache.sling.resourceresolver.unclosedResourceResolvers,type=Metrics",
            List.of("Count")
        );
        return mbeanNamesToAttributes(mbeanNamesToAttributeNames);
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
