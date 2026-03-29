package eu.ciechanowiec.sling.rocket.observation.stats;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NativeStatsTest extends TestEnvironment {

    NativeStatsTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void testDiskStatsCaching() {
        NativeStats nativeStats = context.registerInjectActivateService(NativeStats.class);

        DiskStats stats1 = nativeStats.diskStats();
        DiskStats stats2 = nativeStats.diskStats();

        assertSame(stats1, stats2, "DiskStats should be cached and return the same instance");

        String json = stats1.asJSON();
        assertTrue(json.contains("generatedAt"), "JSON should contain 'generatedAt'");
        assertTrue(json.contains("ttl"), "JSON should contain 'ttl'");
        assertEquals("1 minute", stats1.ttl());
    }
}
