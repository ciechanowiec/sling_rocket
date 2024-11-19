package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.StagedNode;
import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({
        "ClassFanOutComplexity", "MultipleStringLiterals", "PMD.AvoidDuplicateLiterals",
        "PMD.NcssCount", "resource", "OverlyCoupledClass"
})
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
        }, fullResourceAccess).save(realAssetPath);
        Asset firstLink = new StagedAssetLink(realAsset, fullResourceAccess).save(firstLinkPath);
        Asset secondLink = new StagedAssetLink(firstLink, fullResourceAccess).save(secondLinkPath);
        NodeProperties nodeProperties = secondLink.assetMetadata().properties().orElseThrow();
        String filePath = secondLink.assetFile().retrieve().orElseThrow().toPath().toString();
        String mimeType = nodeProperties.propertyValue(AssetMetadata.PN_MIME_TYPE, DefaultProperties.STRING_CLASS)
                .orElseThrow();
        String originalFileName = nodeProperties.propertyValue("originalFileName", DefaultProperties.STRING_CLASS)
                .orElseThrow();
        assertAll(
                () -> assertTrue(filePath.endsWith(".jpg")),
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
        }, fullResourceAccess);
        assertThrows(OccupiedJCRPathException.class, () -> failingAsset.save(realAssetPath));
    }

    @Test
    @SneakyThrows
    @SuppressWarnings({"MethodLength", "EqualsWithItself", "squid:S5863"})
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
        }, fullResourceAccess).save(realAssetPath);
        TargetJCRPath ntFilePath = new TargetJCRPath(new ParentJCRPath(realAssetPath), Asset.FILE_NODE_NAME);
        new NodeProperties(ntFilePath, fullResourceAccess).assertPrimaryType(JcrConstants.NT_FILE);
        TargetJCRPath ntResourcePath = new TargetJCRPath(new ParentJCRPath(ntFilePath), JcrConstants.JCR_CONTENT);
        new NodeProperties(ntResourcePath, fullResourceAccess).assertPrimaryType(JcrConstants.NT_RESOURCE);
        Asset ntFileAsset;
        Asset ntResourceAsset;
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            ntFileAsset = Optional.ofNullable(resourceResolver.getResource(ntFilePath.get()))
                    .map(resource -> new UniversalAsset(resource, fullResourceAccess))
                    .orElseThrow();
            ntResourceAsset = Optional.ofNullable(resourceResolver.getResource(ntResourcePath.get()))
                    .map(resource -> new UniversalAsset(resource, fullResourceAccess))
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
        Asset ntFileLinkOne = new StagedAssetLink(ntFileAsset, fullResourceAccess).save(ntFileLinkPathOne);
        Asset ntFileLinkTwo = new StagedAssetLink(ntFileLinkOne, fullResourceAccess).save(ntFileLinkPathTwo);
        Asset ntResourceLinkOne = new StagedAssetLink(ntResourceAsset, fullResourceAccess).save(ntResourceLinkPathOne);
        Asset ntResourceLinkTwo = new StagedAssetLink(ntResourceLinkOne, fullResourceAccess).save(
                ntResourceLinkPathTwo
        );
        Asset ntFile = new NTFile(ntFilePath, fullResourceAccess);
        assertAll(
                () -> assertEquals(
                        JcrConstants.NT_RESOURCE,
                        ntFile.assetMetadata().properties().orElseThrow().primaryType()
                ),
                () -> assertEquals(
                        Asset.NT_ASSET_METADATA,
                        ntFileAsset.assetMetadata().properties().orElseThrow().primaryType()
                ),
                () -> assertEquals(
                        Asset.NT_ASSET_METADATA,
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
                ),
                () -> assertEquals(ntFile, ntFile),
                () -> assertEquals("image/jpeg", ntFile.assetMetadata().mimeType()),
                () -> assertEquals(5, ntFile.assetMetadata().all().size())
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
        }, fullResourceAccess).save(new TargetJCRPath("/content/separate-asset"));
        StagedNode<Asset> stagedMP3Link = new StagedAssetLink(separateAssetReal, fullResourceAccess);
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
        }, fullResourceAccess);
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
        }, fullResourceAccess);
        TargetJCRPath assetsPath = new TargetJCRPath("/content/assets");
        Assets assets = new StagedAssets(
                List.of(stagedMP3Link, stagedJPGOneReal, stagedJPGTwoReal), fullResourceAccess
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
                List.of(stagedMP3Link, stagedJPGOneReal, stagedJPGTwoReal), fullResourceAccess
        );
        assertThrows(OccupiedJCRPathException.class, () -> failingAssets.save(assetsPath));
    }

    @Test
    void mustThrowWithIllegalNT() {
        context.build()
               .resource("/content/illegal-nt", JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED)
                .commit();
        Resource resource = Optional.ofNullable(context.resourceResolver().getResource("/content/illegal-nt"))
                                    .orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> new UniversalAsset(resource, fullResourceAccess));
    }

    @Test
    void testMetadataFromFile() {
        FileMetadata mp3FM = new FileMetadata(fileMP3);
        FileMetadata jpgFM = new FileMetadata(fileJPGOne);
        Asset assetMP3 = new StagedAssetReal(() -> Optional.of(this.fileMP3), mp3FM, fullResourceAccess).save(
                new TargetJCRPath("/content/mp3")
        );
        Asset assetJPG = new StagedAssetReal(() -> Optional.of(this.fileJPGOne), jpgFM, fullResourceAccess).save(
                new TargetJCRPath("/content/jpg")
        );
        assertAll(
                () -> assertEquals("audio/mpeg", mp3FM.mimeType()),
                () -> assertEquals("image/jpeg", jpgFM.mimeType()),
                () -> assertEquals(NumberUtils.INTEGER_ONE, mp3FM.all().size()),
                () -> assertEquals(NumberUtils.INTEGER_ONE, jpgFM.all().size()),
                () -> assertTrue(mp3FM.properties().isEmpty()),
                () -> assertTrue(jpgFM.properties().isEmpty()),
                () -> assertEquals("audio/mpeg", assetMP3.assetMetadata().mimeType()),
                () -> assertEquals("image/jpeg", assetJPG.assetMetadata().mimeType()),
                () -> assertEquals(
                        Asset.NT_ASSET_METADATA, assetMP3.assetMetadata().all().get(JcrConstants.JCR_PRIMARYTYPE)
                ),
                () -> assertEquals(
                        Asset.NT_ASSET_METADATA, assetJPG.assetMetadata().all().get(JcrConstants.JCR_PRIMARYTYPE)
                )
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    void testAssetsRepository() {
        FileMetadata mp3FM = new FileMetadata(fileMP3);
        FileMetadata jpgFM = new FileMetadata(fileJPGOne);
        Asset assetMP3 = new StagedAssetReal(() -> Optional.of(this.fileMP3), mp3FM, fullResourceAccess).save(
                new TargetJCRPath("/content/mp3")
        );
        Asset assetJPGReal = new StagedAssetReal(() -> Optional.of(this.fileJPGOne), jpgFM, fullResourceAccess).save(
                new TargetJCRPath("/content/jpgOneReal")
        );
        Asset assetJPGLink = new StagedAssetLink(assetJPGReal, fullResourceAccess).save(
                new TargetJCRPath("/content/jpgOneLink")
        );
        Asset assetSeparatePathOne = new StagedAssetReal(() -> Optional.of(this.fileMP3), mp3FM, fullResourceAccess)
                .save(new TargetJCRPath("/content/separate-path/mp3One"));
        Asset assetSeparatePathTwo = new StagedAssetReal(() -> Optional.of(this.fileMP3), mp3FM, fullResourceAccess)
                .save(new TargetJCRPath("/content/separate-path/mp3Two"));
        Asset assetSeparateSubPathOne = new StagedAssetReal(
                () -> Optional.of(this.fileMP3), mp3FM, fullResourceAccess
        ).save(
                new TargetJCRPath("/content/separate-path/sub-path/mp3One")
        );
        Asset assetSeparateSubPathTwo = new StagedAssetReal(
                () -> Optional.of(this.fileMP3), mp3FM, fullResourceAccess
        ).save(
                new TargetJCRPath("/content/separate-path/sub-path/mp3Two")
        );
        AssetsRepository assetsRepository = Optional.ofNullable(context.getService(AssetsRepository.class))
                                                    .orElseThrow();
        assertAll(
                () -> assertEquals(
                        "/content/mp3", assetsRepository.find(assetMP3).orElseThrow().jcrPath().get()
                ),
                () -> assertEquals(
                        "/content/jpgOneReal", assetsRepository.find(assetJPGReal).orElseThrow().jcrPath().get()
                ),
                () -> assertEquals(
                        "/content/jpgOneLink", assetsRepository.find(assetJPGLink).orElseThrow().jcrPath().get()
                ),
                () -> assertEquals(
                        "/content/separate-path/mp3One",
                        assetsRepository.find(assetSeparatePathOne).orElseThrow().jcrPath().get()
                ),
                () -> assertEquals(
                        "/content/separate-path/mp3Two",
                        assetsRepository.find(assetSeparatePathTwo).orElseThrow().jcrPath().get()
                ),
                () -> assertEquals(
                        "/content/separate-path/sub-path/mp3One",
                        assetsRepository.find(assetSeparateSubPathOne).orElseThrow().jcrPath().get()
                ),
                () -> assertEquals(
                        "/content/separate-path/sub-path/mp3Two",
                        assetsRepository.find(assetSeparateSubPathTwo).orElseThrow().jcrPath().get()
                ),
                () -> assertEquals(7, assetsRepository.all().size()),
                () -> assertEquals(7, assetsRepository.find(new TargetJCRPath("/")).size()),
                () -> assertEquals(4, assetsRepository.find(new TargetJCRPath("/content/separate-path")).size()),
                () -> assertTrue(assetsRepository.find((Referencable) () -> "non-existent-uuid").isEmpty())
        );
    }

    @SuppressWarnings("MagicNumber")
    @SneakyThrows
    @Test
    void testSizeFromRepository() {
        // Create temporary files with known size
        File tempFileOne = File.createTempFile("testFileOne", ".tmp");
        File tempFileTwo = File.createTempFile("testFileOne", ".tmp");
        tempFileOne.deleteOnExit();
        tempFileTwo.deleteOnExit();

        byte[] data = new byte[1024]; // 1 KB
        try (
                OutputStream fosOne = Files.newOutputStream(tempFileOne.toPath());
                OutputStream fosTwo = Files.newOutputStream(tempFileTwo.toPath())
        ) {
            IOUtils.write(data, fosOne);
            IOUtils.write(data, fosTwo);
        }
        new StagedAssetReal(() -> Optional.of(tempFileOne), new FileMetadata(tempFileOne), fullResourceAccess).save(
                new TargetJCRPath("/content/tempFileOne")
        );
        new StagedAssetReal(() -> Optional.of(tempFileOne), new FileMetadata(tempFileOne), fullResourceAccess).save(
                new TargetJCRPath("/content/tempFileTwo")
        );
        AssetsRepository assetsRepository = Optional.ofNullable(context.getService(AssetsRepository.class))
                .orElseThrow();
        assertEquals(new DataSize(2, DataUnit.KILOBYTES), assetsRepository.size());
    }

    @Test
    @SuppressWarnings("MethodLength")
    void testFileNameExtension() {
        FileMetadata mp3FM = new FileMetadata(fileMP3);
        FileMetadata jpgFM = new FileMetadata(fileJPGOne);
        File compiledJavaToSave = loadResourceIntoFile("compiled-java-file");
        FileMetadata compiledJavaToSaveFM = new FileMetadata(compiledJavaToSave);
        Asset assetMP3 = new StagedAssetReal(() -> Optional.of(this.fileMP3), mp3FM, fullResourceAccess).save(
                new TargetJCRPath("/content/mp3")
        );
        Asset compiledJava = new StagedAssetReal(
                () -> Optional.of(compiledJavaToSave), compiledJavaToSaveFM, fullResourceAccess
        ).save(new TargetJCRPath("/content/compiledJava"));
        Asset assetJPGReal = new StagedAssetReal(() -> Optional.of(this.fileJPGOne), jpgFM, fullResourceAccess).save(
                new TargetJCRPath("/content/jpgOneReal")
        );
        Asset assetJPGLink = new StagedAssetLink(assetJPGReal, fullResourceAccess).save(
                new TargetJCRPath("/content/jpgOneLink")
        );
        AssetMetadata nonExistentMimeType = new AssetMetadata() {

            @Override
            public String mimeType() {
                return "non-existent-mime-type";
            }

            @Override
            public Map<String, String> all() {
                return Map.of();
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.empty();
            }
        };
        AssetMetadata wildCardMimeType = new AssetMetadata() {

            @Override
            public String mimeType() {
                return MediaType.WILDCARD;
            }

            @Override
            public Map<String, String> all() {
                return Map.of();
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.empty();
            }
        };
        assertAll(
                () -> assertEquals(".mpga", assetMP3.assetMetadata().filenameExtension().orElseThrow()),
                () -> assertEquals(".jpg", assetJPGReal.assetMetadata().filenameExtension().orElseThrow()),
                () -> assertEquals(".jpg", assetJPGLink.assetMetadata().filenameExtension().orElseThrow()),
                () -> assertEquals(".class", compiledJava.assetMetadata().filenameExtension().orElseThrow()),
                () -> assertTrue(nonExistentMimeType.filenameExtension().isEmpty()),
                () -> assertTrue(wildCardMimeType.filenameExtension().isEmpty())
        );
    }
}
