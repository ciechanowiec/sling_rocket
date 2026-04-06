package eu.ciechanowiec.sling.rocket.observation.stats;

import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import lombok.SneakyThrows;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"MultipleStringLiterals", "MagicNumber", "PMD.AvoidDuplicateLiterals"})
class JMXStatsTest extends TestEnvironment {

    private final Collection<ObjectName> namesToUnregister = new HashSet<>();

    JMXStatsTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @SuppressWarnings({"MethodLength", "PMD.ReplaceJavaUtilDate"})
    @BeforeEach
    void setup() {
        registerMBean(
            "org.apache.jackrabbit.oak:name=FileStore statistics,type=FileStoreStats", Map.of(
                "SegmentCount", 100,
                "ApproximateSize", 1024L,
                "ApproximateSizeReadable", "1 KB",
                "TarFileCount", 5
            )
        );
        registerMBean("org.apache.jackrabbit.oak:name=NODE_COUNT_FROM_ROOT,type=Metrics", Map.of("Count", 500L));
        registerMBean("org.apache.jackrabbit.oak:name=SESSION_LOGIN_COUNTER,type=Metrics", Map.of("Count", 10L));
        registerMBean("org.apache.jackrabbit.oak:name=SESSION_WRITE_COUNTER,type=Metrics", Map.of("Count", 20L));
        registerMBean("org.apache.jackrabbit.oak:name=SESSION_COUNT,type=Metrics", Map.of("Count", 30L));
        registerMBean("org.apache.jackrabbit.oak:name=COMMITS_COUNT,type=Metrics", Map.of("Count", 40L));
        registerMBean(
            "org.apache.jackrabbit.oak:name=COMMIT_TIME,type=Metrics", Map.of(
                "Mean", 1.5,
                "DurationUnit", "milliseconds",
                "Count", 100L
            )
        );
        registerMBean(
            "org.apache.jackrabbit.oak:name=QUERY_DURATION,type=Metrics", Map.of(
                "Mean", 2.5,
                "DurationUnit", "milliseconds",
                "Count", 200L
            )
        );
        registerMBean(
            "org.apache.jackrabbit.oak:name=\"QUERY_DURATION;index=uuid\",type=Metrics", Map.of(
                "Mean", 3.5,
                "DurationUnit", "milliseconds",
                "Count", 300L
            )
        );
        registerMBean(
            "org.apache.sling:type=engine,service=RequestProcessor", Map.of(
                "RequestsCount", 1000L,
                "MeanRequestDurationMsec", 15.0
            )
        );
        Map<String, Object> allQueuesAttributes = new ConcurrentHashMap<>();
        allQueuesAttributes.put("StartDate", new Date());
        allQueuesAttributes.put("LastFinishedJobDate", new Date());
        allQueuesAttributes.put("NumberOfJobs", 5);
        allQueuesAttributes.put("NumberOfQueuedJobs", 2);
        allQueuesAttributes.put("NumberOfActiveJobs", 1);
        allQueuesAttributes.put("NumberOfFinishedJobs", 100);
        allQueuesAttributes.put("NumberOfFailedJobs", 0);
        allQueuesAttributes.put("NumberOfCancelledJobs", 1);
        allQueuesAttributes.put("NumberOfProcessedJobs", 101);
        allQueuesAttributes.put("AverageProcessingTime", 50L);
        allQueuesAttributes.put("AverageWaitingTime", 10L);
        registerMBean("org.apache.sling:name=AllQueues,type=queues", allQueuesAttributes);
        registerMBean(
            "org.apache.sling.auth.core:name=sling.auth.core.authenticate.timer,type=Metrics", Map.of(
                "Mean", 5.0,
                "50thPercentile", 4.0,
                "95thPercentile", 10.0,
                "DurationUnit", "milliseconds",
                "Count", 500L
            )
        );
        registerMBean(
            "org.apache.sling.auth.core:name=sling.auth.core.authenticate.failed,type=Metrics", Map.of("Count", 2L)
        );
        registerMBean(
            "org.apache.sling.resourceresolver:"
                + "name=org.apache.sling.resourceresolver.unclosedResourceResolvers,type=Metrics",
            Map.of("Count", 1L)
        );
    }

    @SneakyThrows
    @AfterEach
    void teardown() {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        namesToUnregister.stream()
            .filter(mbeanServer::isRegistered)
            .forEach(SneakyConsumer.sneaky(mbeanServer::unregisterMBean));
        namesToUnregister.clear();
    }

    @Test
    void testJMXStats() {
        JMXStats jmxStats = new JMXStats();

        assertEquals(JMXStats.class.getName(), jmxStats.name());

        String json = jmxStats.asJSON();

        assertAll(
            () -> assertTrue(json.contains("java.lang"), "Should contain java.lang"),
            () -> assertTrue(json.contains("org.apache.jackrabbit.oak"), "Should contain oak"),
            () -> assertTrue(json.contains("org.apache.sling"), "Should contain sling"),
            () -> assertTrue(json.contains("org.apache.sling.auth.core"), "Should contain sling.auth.core"),
            () -> assertTrue(json.contains("org.apache.sling.resourceresolver"), "Should contain resourceresolver"),
            () -> assertTrue(json.contains("ThreadCount"), "Should contain ThreadCount"),
            () -> assertTrue(json.contains("Name"), "Should contain StartTimeReadable"),
            () -> assertTrue(json.contains("StartTimeReadable"), "Should contain StartTimeReadable"),
            () -> assertTrue(json.contains("SegmentCount"), "Should contain SegmentCount"),
            () -> assertTrue(json.contains("1024"), "Should contain ApproximateSize value"),
            () -> assertTrue(json.contains("1 KB"), "Should contain ApproximateSizeReadable value"),
            () -> assertTrue(json.contains("QUERY_DURATION;index=uuid"), "Should contain quoted MBean name"),
            () -> assertTrue(
                json.contains("TimeElapsedFromStartReadable"), "Should contain TimeElapsedFromStartReadable"
            ),
            () -> assertTrue(
                json.contains("days,") && json.contains("hours,"), "Should have correct duration format"
            )
        );
    }

    @Test
    void testMissingMBeans() {
        // Unregister everything to test N/A
        teardown();

        JSON jmxStats = new JMXStats();
        String json = jmxStats.asJSON();

        // Standard java.lang MBeans should still be there, but Oak/Sling should be N/A
        assertTrue(json.contains("N/A ["), "Should contain N/A for missing MBeans: " + json);
    }

    @SneakyThrows
    private void registerMBean(String name, Map<String, Object> attributes) {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName(name);
        if (mbeanServer.isRegistered(objectName)) {
            mbeanServer.unregisterMBean(objectName);
        }
        mbeanServer.registerMBean(new TestMBean(attributes), objectName);
        namesToUnregister.add(objectName);
    }

    private record TestMBean(Map<String, Object> attributes) implements DynamicMBean {

        @Override
        public Object getAttribute(String attribute) {
            return attributes.get(attribute);
        }

        @Override
        public void setAttribute(Attribute attribute) {
            // No-op for testing
        }

        @Override
        @SuppressWarnings("PMD.LooseCoupling")
        public AttributeList getAttributes(String[] attributes) {
            AttributeList attributeList = new AttributeList();
            Arrays.stream(attributes)
                .filter(this.attributes::containsKey)
                .forEach(name -> attributeList.add(new Attribute(name, this.attributes.get(name))));
            return attributeList;
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes) {
            return new AttributeList();
        }

        @Override
        public Object invoke(String actionName, Object[] params, String[] signature) {
            return new Object();
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            MBeanAttributeInfo[] attributesFoMBeanInfo = attributes.keySet().stream()
                .map(
                    name -> new MBeanAttributeInfo(
                        name, "java.lang.Object", "description", true, false, false)
                ).toArray(MBeanAttributeInfo[]::new);
            return new MBeanInfo(
                getClass().getName(), "description", attributesFoMBeanInfo, null, null, null
            );
        }
    }
}
