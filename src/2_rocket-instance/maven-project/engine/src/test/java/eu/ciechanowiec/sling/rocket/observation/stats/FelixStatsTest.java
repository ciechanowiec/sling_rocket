package eu.ciechanowiec.sling.rocket.observation.stats;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.ComponentConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("RegexpMultiline")
class FelixStatsTest extends TestEnvironment {

    private static final String BUNDLES_STARTED_CHECK_NAME = "org.apache.felix.hc.generalchecks.BundlesStartedCheck";
    private static final String CPU_CHECK_NAME = "org.apache.felix.hc.generalchecks.CpuCheck";
    private static final String MEMORY_CHECK_NAME = "org.apache.felix.hc.generalchecks.MemoryCheck";

    private BundlesStartedCheckMock bundlesStartedCheck;
    private CpuCheckMock cpuCheck;
    private MemoryCheckMock memoryCheck;

    FelixStatsTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @BeforeEach
    void setup() {
        bundlesStartedCheck = new BundlesStartedCheckMock();
        cpuCheck = new CpuCheckMock();
        memoryCheck = new MemoryCheckMock();

        context.registerService(
            HealthCheck.class, bundlesStartedCheck, Map.of(
                ComponentConstants.COMPONENT_NAME, BUNDLES_STARTED_CHECK_NAME
            )
        );
        context.registerService(
            HealthCheck.class, cpuCheck, Map.of(
                ComponentConstants.COMPONENT_NAME, CPU_CHECK_NAME
            )
        );
        context.registerService(
            HealthCheck.class, memoryCheck, Map.of(
                ComponentConstants.COMPONENT_NAME, MEMORY_CHECK_NAME
            )
        );
    }

    @Test
    void testFelixStats() {
        Result bundlesResult = mock(Result.class);
        when(bundlesResult.getStatus()).thenReturn(Result.Status.OK);
        when(bundlesResult.iterator()).thenReturn(Collections.emptyIterator());
        bundlesStartedCheck.setResult(bundlesResult);

        Result cpuResult = mock(Result.class);
        when(cpuResult.getStatus()).thenReturn(Result.Status.WARN);
        ResultLog.Entry cpuEntry = new ResultLog.Entry(Result.Status.WARN, "High CPU usage");
        when(cpuResult.iterator()).thenReturn(List.of(cpuEntry).iterator());
        cpuCheck.setResult(cpuResult);

        Result memoryResult = mock(Result.class);
        when(memoryResult.getStatus()).thenReturn(Result.Status.CRITICAL);
        ResultLog.Entry memoryEntry1 = new ResultLog.Entry(Result.Status.OK, "Low memory warning");
        ResultLog.Entry memoryEntry2 = new ResultLog.Entry(Result.Status.CRITICAL, "Out of memory");
        when(memoryResult.iterator()).thenReturn(List.of(memoryEntry1, memoryEntry2).iterator());
        memoryCheck.setResult(memoryResult);

        FelixStats felixStats = context.registerInjectActivateService(FelixStats.class);

        assertEquals(FelixStats.class.getName(), felixStats.name());

        String json = felixStats.asJSON();
        assertAll(
            () -> assertTrue(json.contains("BundlesStartedCheckMock")),
            () -> assertTrue(json.contains("[OK]")),
            () -> assertTrue(json.contains("CpuCheckMock")),
            () -> assertTrue(json.contains("[WARN] High CPU usage")),
            () -> assertTrue(json.contains("MemoryCheckMock")),
            () -> assertTrue(json.contains("[CRITICAL] Out of memory"))
        );
    }

    @Test
    void testFelixStatsWithEmptyEntries() {
        Result emptyResult = mock(Result.class);
        when(emptyResult.getStatus()).thenReturn(Result.Status.TEMPORARILY_UNAVAILABLE);
        when(emptyResult.iterator()).thenReturn(Collections.emptyIterator());

        bundlesStartedCheck.setResult(emptyResult);
        cpuCheck.setResult(emptyResult);
        memoryCheck.setResult(emptyResult);

        FelixStats felixStats = context.registerInjectActivateService(FelixStats.class);

        String json = felixStats.asJSON();
        // Since there are no entries, the message part should be empty.
        // String format is "[%s] %s"
        assertTrue(json.contains("[TEMPORARILY_UNAVAILABLE]"));
    }

    @SuppressWarnings("AbstractClassName")
    private abstract static class BaseMockHealthCheck implements HealthCheck {

        @SuppressWarnings({"RedundantFieldInitialization", "ExplicitInitialization", "PMD.RedundantFieldInitializer"})
        private Result result = null;

        void setResult(Result result) {
            this.result = result;
        }

        @Override
        public Result execute() {
            return result;
        }
    }

    private static final class BundlesStartedCheckMock extends BaseMockHealthCheck {

    }

    private static final class CpuCheckMock extends BaseMockHealthCheck {

    }

    private static final class MemoryCheckMock extends BaseMockHealthCheck {

    }
}
