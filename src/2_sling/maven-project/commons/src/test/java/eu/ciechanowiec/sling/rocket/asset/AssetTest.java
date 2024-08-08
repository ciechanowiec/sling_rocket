package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("MultipleStringLiterals")
class AssetTest extends TestEnvironment {

    private File file;

    AssetTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @SneakyThrows
    @BeforeEach
    void setup() {
        file = File.createTempFile("jcr-binary_", ".tmp");
        file.deleteOnExit();
        Path tempFilePath = file.toPath();
        Thread currentThread = Thread.currentThread();
        ClassLoader classLoader = currentThread.getContextClassLoader();
        try (
                InputStream inputStream = Optional.ofNullable(
                        classLoader.getResourceAsStream("1.jpeg")
                ).orElseThrow();
                OutputStream outputStream = Files.newOutputStream(tempFilePath)
        ) {
            IOUtils.copy(inputStream, outputStream);
        }
        assertTrue(file.exists());
    }

    @Test
    @SneakyThrows
    void mustSaveAndRetrieveAssets() {
        TargetJCRPath realAssetPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        TargetJCRPath firstLinkPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        TargetJCRPath secondLinkPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        Asset realAsset = new StagedAssetReal(() -> Optional.of(file), new SimpleMetadata() {
            @Override
            public String mimeType() {
                return "image/jpeg";
            }

            @Override
            public Map<String, String> all() {
                return Map.of(PN_MIME_TYPE, mimeType(), "originalFileName", "originalus");
            }
        }, resourceAccess).save(realAssetPath);
        Asset firstLink = new StagedAssetLink(realAsset, resourceAccess).save(firstLinkPath);
        Asset secondLink = new StagedAssetLink(firstLink, resourceAccess).save(secondLinkPath);
        NodeProperties nodeProperties = secondLink.assetMetadata().retrieve();
        String filePath = secondLink.assetFile().retrieve().orElseThrow().toPath().toString();
        String mimeType = nodeProperties.propertyValue(SimpleMetadata.PN_MIME_TYPE, DefaultProperties.STRING_CLASS)
                .orElseThrow();
        String originalFileName = nodeProperties.propertyValue("originalFileName", DefaultProperties.STRING_CLASS)
                .orElseThrow();
        assertAll(
                () -> assertTrue(filePath.contains("jcr-binary_")),
                () -> assertTrue(filePath.contains(".tmp")),
                () -> assertEquals("originalus", originalFileName),
                () -> assertEquals("image/jpeg", mimeType)
        );

        StagedAssetReal failingAsset = new StagedAssetReal(() -> Optional.of(file), new SimpleMetadata() {
            @Override
            public String mimeType() {
                return "image/jpeg";
            }

            @Override
            public Map<String, String> all() {
                return Map.of(PN_MIME_TYPE, mimeType(), "originalFileName", "originalus");
            }
        }, resourceAccess);
        assertThrows(OccupiedJCRPathException.class, () -> failingAsset.save(realAssetPath));
    }
}
