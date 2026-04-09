package eu.ciechanowiec.sling.rocket.observation.stats;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RunModesStatsTest extends TestEnvironment {

    RunModesStatsTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @Test
    void testRunModesStats() {
        context.runMode("xxo", "stage", "anno");
        RunModesStats runModesStats = context.registerInjectActivateService(RunModesStats.class);
        SortedSet<String> actualRunModes = runModesStats.runModes();
        assertAll(
            () -> assertEquals(new TreeSet<>(List.of("xxo", "stage", "anno")), actualRunModes),
            () -> assertEquals("{\"runModes\":[\"anno\",\"stage\",\"xxo\"]}", runModesStats.asJSON()),
            () -> assertEquals(RunModesStats.class.getName(), runModesStats.name())
        );
    }
}
