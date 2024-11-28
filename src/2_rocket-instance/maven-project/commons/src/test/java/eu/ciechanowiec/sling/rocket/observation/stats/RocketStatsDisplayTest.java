package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RocketStatsDisplayTest extends TestEnvironment {

    RocketStatsDisplayTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @SuppressWarnings({"LineLength", "MethodLength"})
    @Test
    void basicTest() {
        File jpeg = loadResourceIntoFile("1.jpeg");
        File mp3 = loadResourceIntoFile("time-forward.mp3");
        new StagedAssetReal(() -> Optional.of(jpeg), new FileMetadata(jpeg), fullResourceAccess).save(
                new TargetJCRPath("/content/jpeg")
        );
        new StagedAssetReal(() -> Optional.of(mp3), new FileMetadata(mp3), fullResourceAccess).save(
                new TargetJCRPath("/content/mp3")
        );
        RocketStats customStats = new RocketStats() {
            @Override
            public String name() {
                return "CustomStats";
            }

            @JsonProperty("customData")
            String customData() {
                return "This is some custom data";
            }

            @Override
            public String asJSON() {
                return "{\"datus\":\"%s\"}".formatted(customData());
            }
        };
        context.registerService(RocketStats.class, customStats);
        context.registerInjectActivateService(NativeStats.class);
        RocketStatsDisplay rocketStatsDisplay = context.registerInjectActivateService(RocketStatsDisplay.class);
        String actualJson = rocketStatsDisplay.asJSON();
        assertAll(
                () -> assertEquals("{\"datus\":\"This is some custom data\"}", customStats.asJSON()),
                () -> assertTrue(actualJson.contains("NativeStats")),
                () -> assertTrue(actualJson.contains("rocketStats")),
                () -> assertTrue(actualJson.contains("eu.ciechanowiec.sling.rocket.observation.stats.NativeStats")),
                () -> assertTrue(actualJson.contains("diskStats")),
                () -> assertTrue(actualJson.contains("totalSpace")),
                () -> assertTrue(actualJson.contains("occupiedSpace")),
                () -> assertTrue(actualJson.contains("freeSpace")),
                () -> assertTrue(actualJson.contains("assetsStats")),
                () -> assertTrue(actualJson.contains("numberOfAllAssets")),
                () -> assertTrue(actualJson.contains("dataSizeOfAllAssets")),
                () -> assertTrue(actualJson.contains("averageAssetSize")),
                () -> assertTrue(actualJson.contains("biggestAssets")),
                () -> assertTrue(actualJson.contains("CustomStats")),
                () -> assertTrue(actualJson.contains("customData")),
                () -> assertTrue(actualJson.contains("This is some custom data")),
                () -> assertTrue(actualJson.contains("generationTime"))
        );
    }
}
