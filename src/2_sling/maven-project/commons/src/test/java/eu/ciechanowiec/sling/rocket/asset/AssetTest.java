package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.StagedNode;
import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals", "PMD.NcssCount"})
class AssetTest extends TestEnvironment {

    private File fileJPGOne;
    private File fileJPGTwo;
    private File fileMP3;

    AssetTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @SneakyThrows
    @BeforeEach
    void setup() {
        fileJPGOne = loadResourceIntoFile("1.jpeg");
        fileJPGTwo = loadResourceIntoFile("2.jpeg");
        fileMP3 = loadResourceIntoFile("time-forward.mp3");
    }

    @SneakyThrows
    private File loadResourceIntoFile(String resourceName) {
        File createdFile = File.createTempFile("jcr-binary_", ".tmp");
        createdFile.deleteOnExit();
        Path tempFilePath = createdFile.toPath();
        Thread currentThread = Thread.currentThread();
        ClassLoader classLoader = currentThread.getContextClassLoader();
        try (
                InputStream inputStream = Optional.ofNullable(
                        classLoader.getResourceAsStream(resourceName)
                ).orElseThrow();
                OutputStream outputStream = Files.newOutputStream(tempFilePath)
        ) {
            IOUtils.copy(inputStream, outputStream);
        }
        assertTrue(createdFile.exists());
        return createdFile;
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("MethodLength")
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
        Asset realAsset = new StagedAssetReal(() -> Optional.of(fileJPGOne), new AssetMetadata() {
            @Override
            public String mimeType() {
                return "image/jpeg";
            }

            @Override
            public Map<String, String> all() {
                return Map.of(PN_MIME_TYPE, mimeType(), "originalFileName", "originalus");
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.empty();
            }
        }, resourceAccess).save(realAssetPath);
        Asset firstLink = new StagedAssetLink(realAsset, resourceAccess).save(firstLinkPath);
        Asset secondLink = new StagedAssetLink(firstLink, resourceAccess).save(secondLinkPath);
        NodeProperties nodeProperties = secondLink.assetMetadata().properties().orElseThrow();
        String filePath = secondLink.assetFile().retrieve().orElseThrow().toPath().toString();
        String mimeType = nodeProperties.propertyValue(AssetMetadata.PN_MIME_TYPE, DefaultProperties.STRING_CLASS)
                .orElseThrow();
        String originalFileName = nodeProperties.propertyValue("originalFileName", DefaultProperties.STRING_CLASS)
                .orElseThrow();
        assertAll(
                () -> assertTrue(filePath.contains("jcr-binary_")),
                () -> assertTrue(filePath.contains(".tmp")),
                () -> assertEquals("originalus", originalFileName),
                () -> assertEquals("image/jpeg", mimeType)
        );

        StagedAssetReal failingAsset = new StagedAssetReal(() -> Optional.of(fileJPGOne), new AssetMetadata() {
            @Override
            public String mimeType() {
                return "image/jpeg";
            }

            @Override
            public Map<String, String> all() {
                return Map.of(PN_MIME_TYPE, mimeType(), "originalFileName", "originalus");
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.empty();
            }
        }, resourceAccess);
        assertThrows(OccupiedJCRPathException.class, () -> failingAsset.save(realAssetPath));
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("MethodLength")
    void mustRetrieveFromNTFile() {
        TargetJCRPath realAssetPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        new StagedAssetReal(() -> Optional.of(fileJPGOne), new AssetMetadata() {
            @Override
            public String mimeType() {
                return "image/jpeg";
            }

            @Override
            public Map<String, String> all() {
                return Map.of(PN_MIME_TYPE, mimeType(), "originalFileName", "originalus");
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.empty();
            }
        }, resourceAccess).save(realAssetPath);
        TargetJCRPath ntFilePath = new TargetJCRPath(new ParentJCRPath(realAssetPath), Asset.FILE_NODE_NAME);
        new NodeProperties(ntFilePath, resourceAccess).assertPrimaryType(JcrConstants.NT_FILE);
        TargetJCRPath ntResourcePath = new TargetJCRPath(new ParentJCRPath(ntFilePath), JcrConstants.JCR_CONTENT);
        new NodeProperties(ntResourcePath, resourceAccess).assertPrimaryType(JcrConstants.NT_RESOURCE);
        Asset ntFileAsset;
        Asset ntResourceAsset;
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            ntFileAsset = Optional.ofNullable(resourceResolver.getResource(ntFilePath.get()))
                    .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Asset.class)))
                    .orElseThrow();
            ntResourceAsset = Optional.ofNullable(resourceResolver.getResource(ntResourcePath.get()))
                    .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Asset.class)))
                    .orElseThrow();
        }
        TargetJCRPath ntFileLinkPathOne = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        TargetJCRPath ntFileLinkPathTwo = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        TargetJCRPath ntResourceLinkPathOne = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        TargetJCRPath ntResourceLinkPathTwo = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        Asset ntFileLinkOne = new StagedAssetLink(ntFileAsset, resourceAccess).save(ntFileLinkPathOne);
        Asset ntFileLinkTwo = new StagedAssetLink(ntFileLinkOne, resourceAccess).save(ntFileLinkPathTwo);
        Asset ntResourceLinkOne = new StagedAssetLink(ntResourceAsset, resourceAccess).save(ntResourceLinkPathOne);
        Asset ntResourceLinkTwo = new StagedAssetLink(ntResourceLinkOne, resourceAccess).save(ntResourceLinkPathTwo);
        assertAll(
                () -> assertEquals(
                        JcrConstants.NT_RESOURCE,
                        ntFileAsset.assetMetadata().properties().orElseThrow().primaryType()
                ),
                () -> assertEquals(
                        JcrConstants.NT_RESOURCE,
                        ntResourceAsset.assetMetadata().properties().orElseThrow().primaryType()
                ),
                () -> assertEquals(
                        ntFileAsset.assetMetadata().properties().orElseThrow().all(),
                        ntResourceAsset.assetMetadata().properties().orElseThrow().all()
                ),
                () -> assertEquals(
                        ntFileAsset.assetMetadata().all(),
                        ntResourceAsset.assetMetadata().all()
                ),
                () -> assertEquals(
                        ntFileAsset.jcrPath(),
                        ntResourceAsset.jcrPath()
                )
        );
        Set<Map<String, String>> all = Set.copyOf(List.of(
                ntFileAsset.assetMetadata().all(),
                ntResourceAsset.assetMetadata().all(),
                ntFileLinkOne.assetMetadata().all(),
                ntFileLinkTwo.assetMetadata().all(),
                ntResourceLinkOne.assetMetadata().all(),
                ntResourceLinkTwo.assetMetadata().all()
        ));
        assertEquals(NumberUtils.INTEGER_ONE, all.size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    void mustSaveAssetsInBulk() {
        Asset separateAssetReal = new StagedAssetReal(() -> Optional.of(fileMP3), new AssetMetadata() {
            @Override
            public String mimeType() {
                return "audio/mpeg";
            }

            @Override
            public Map<String, String> all() {
                return Map.of(PN_MIME_TYPE, mimeType(), "originalFileName", "MP3OriginalName");
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.empty();
            }
        }, resourceAccess).save(new TargetJCRPath("/content/separate-asset"));
        StagedNode<Asset> stagedMP3Link = new StagedAssetLink(separateAssetReal, resourceAccess);
        StagedNode<Asset> stagedJPGOneReal = new StagedAssetReal(
                () -> Optional.of(fileJPGOne), new AssetMetadata() {
            @Override
            public String mimeType() {
                return "image/jpeg";
            }

            @Override
            public Map<String, String> all() {
                return Map.of(PN_MIME_TYPE, mimeType(), "originalFileName", "JPGOneOriginalName");
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.empty();
            }
        }, resourceAccess);
        StagedNode<Asset> stagedJPGTwoReal = new StagedAssetReal(
                () -> Optional.of(fileJPGTwo), new AssetMetadata() {
            @Override
            public String mimeType() {
                return "image/jpeg";
            }

            @Override
            public Map<String, String> all() {
                return Map.of(PN_MIME_TYPE, mimeType(), "originalFileName", "JPGTwoOriginalName");
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.empty();
            }
        }, resourceAccess);
        TargetJCRPath assetsPath = new TargetJCRPath("/content/assets");
        Assets assets = new StagedAssets(
                List.of(stagedMP3Link, stagedJPGOneReal, stagedJPGTwoReal), resourceAccess
        ).save(assetsPath);
        Set<String> originalFileNames = assets.get()
                .stream()
                .map(Asset::assetMetadata)
                .map(AssetMetadata::properties)
                .flatMap(Optional::stream)
                .map(nodeProperties -> nodeProperties.propertyValue("originalFileName", DefaultProperties.STRING_CLASS))
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
        int numOfAssets = assets.get().size();
        assertAll(
                () -> assertEquals(3, numOfAssets),
                () -> assertTrue(originalFileNames.contains("MP3OriginalName")),
                () -> assertTrue(originalFileNames.contains("JPGOneOriginalName")),
                () -> assertTrue(originalFileNames.contains("JPGTwoOriginalName"))
        );

        StagedAssets failingAssets = new StagedAssets(
                List.of(stagedMP3Link, stagedJPGOneReal, stagedJPGTwoReal), resourceAccess
        );
        assertThrows(OccupiedJCRPathException.class, () -> failingAssets.save(assetsPath));
    }
}
