package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.asset.UsualFileAsAssetFile;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.io.File;

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
        new StagedAssetReal(new UsualFileAsAssetFile(jpeg), new FileMetadata(jpeg), fullResourceAccess).save(
            new TargetJCRPath("/content/jpeg")
        );
        new StagedAssetReal(new UsualFileAsAssetFile(mp3), new FileMetadata(mp3), fullResourceAccess).save(
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
        context.registerInjectActivateService(DiskStats.class);
        context.registerInjectActivateService(AssetsStats.class);
        RocketStatsDisplay rocketStatsDisplay = context.registerInjectActivateService(RocketStatsDisplay.class);
        String actualJson = rocketStatsDisplay.asJSON();
        assertAll(
            () -> assertEquals("{\"datus\":\"This is some custom data\"}", customStats.asJSON()),
            () -> assertTrue(actualJson.contains("rocketStats")),
            () -> assertTrue(actualJson.contains("eu.ciechanowiec.sling.rocket.observation.stats.DiskStats")),
            () -> assertTrue(actualJson.contains("totalSpaceBytes")),
            () -> assertTrue(actualJson.contains("totalSpaceReadable")),
            () -> assertTrue(actualJson.contains("occupiedSpaceBytes")),
            () -> assertTrue(actualJson.contains("occupiedSpaceReadable")),
            () -> assertTrue(actualJson.contains("usableSpaceBytes")),
            () -> assertTrue(actualJson.contains("usableSpaceReadable")),
            () -> assertTrue(actualJson.contains("eu.ciechanowiec.sling.rocket.observation.stats.AssetsStats")),
            () -> assertTrue(actualJson.contains("numberOfAllAssets")),
            () -> assertTrue(actualJson.contains("dataSizeOfAllAssetsBytes")),
            () -> assertTrue(actualJson.contains("dataSizeOfAllAssetsReadable")),
            () -> assertTrue(actualJson.contains("averageAssetSizeBytes")),
            () -> assertTrue(actualJson.contains("averageAssetSizeReadable")),
            () -> assertTrue(actualJson.contains("biggestAssets")),
            () -> assertTrue(actualJson.contains("CustomStats")),
            () -> assertTrue(actualJson.contains("customData")),
            () -> assertTrue(actualJson.contains("This is some custom data")),
            () -> assertTrue(actualJson.contains("generationTime"))
        );
    }
}
