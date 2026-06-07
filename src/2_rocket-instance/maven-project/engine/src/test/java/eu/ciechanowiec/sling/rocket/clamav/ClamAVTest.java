package eu.ciechanowiec.sling.rocket.clamav;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetFile;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@SuppressWarnings({"MagicNumber", "MultipleStringLiterals", "PMD.TooManyStaticImports"})
class ClamAVTest extends TestEnvironment {

    ClamAVTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    private ClamAV clamAVFor(int port) {
        return context.registerInjectActivateService(
            ClamAV.class, Map.of(
                "clamav.host", "localhost",
                "clamav.port", port,
                "clamav.connect-timeout", 2_000,
                "clamav.read-timeout", 10_000
            )
        );
    }

    private InputStream contentOf(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void mustDetectCleanContent() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.EICAR_AUTO)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            ScanResult scanResult = clamAV.scan(contentOf("This content is perfectly innocent"));
            assertAll(
                () -> assertInstanceOf(Clean.class, scanResult),
                () -> assertEquals("CLEAN", scanResult.summary())
            );
        }
    }

    @Test
    void mustDetectInfectedContent() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.EICAR_AUTO)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            ScanResult scanResult = clamAV.scan(contentOf(FakeClamd.EICAR));
            assertAll(
                () -> assertInstanceOf(Infected.class, scanResult),
                () -> assertEquals(FakeClamd.EICAR_SIGNATURE_NAME, ((Infected) scanResult).signatureName()),
                () -> assertEquals("INFECTED: " + FakeClamd.EICAR_SIGNATURE_NAME, scanResult.summary())
            );
        }
    }

    @Test
    void mustScanMultiChunkContent() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.EICAR_AUTO)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            byte[] bigBenignContent = new byte[200_000];
            ScanResult scanResult = clamAV.scan(new ByteArrayInputStream(bigBenignContent));
            assertInstanceOf(Clean.class, scanResult);
        }
    }

    @Test
    void mustFailOnSizeLimit() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.SIZE_LIMIT)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            ScanResult scanResult = clamAV.scan(contentOf("Some content beyond all reasonable size limits"));
            assertAll(
                () -> assertInstanceOf(Failed.class, scanResult),
                () -> assertTrue(((Failed) scanResult).details().contains("size limit")),
                () -> assertTrue(scanResult.summary().startsWith("FAILED: "))
            );
        }
    }

    @Test
    void mustFailOnMidStreamReset() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.RESET_MIDSTREAM)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            byte[] bigContent = new byte[2_000_000];
            ScanResult scanResult = clamAV.scan(new ByteArrayInputStream(bigContent));
            assertInstanceOf(Failed.class, scanResult);
        }
    }

    @Test
    void mustFailOnGarbageReply() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.GARBAGE)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            ScanResult scanResult = clamAV.scan(contentOf("Whatever content"));
            assertAll(
                () -> assertInstanceOf(Failed.class, scanResult),
                () -> assertEquals("BLAH-BLAH", ((Failed) scanResult).details()),
                () -> assertFalse(clamAV.ping())
            );
        }
    }

    @Test
    void mustFailOnNoReply() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.NO_REPLY)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            ScanResult scanResult = clamAV.scan(contentOf("Whatever content"));
            assertAll(
                () -> assertInstanceOf(Failed.class, scanResult),
                () -> assertEquals("Empty reply from clamd", ((Failed) scanResult).details()),
                () -> assertTrue(clamAV.version().isEmpty())
            );
        }
    }

    @SneakyThrows
    private int unusedPort() {
        try (ServerSocket throwawaySocket = new ServerSocket(0)) {
            return throwawaySocket.getLocalPort();
        }
    }

    @Test
    void mustFailWhenUnreachable() {
        ClamAV clamAV = clamAVFor(unusedPort());
        ScanResult scanResult = clamAV.scan(contentOf("Whatever content"));
        String statsAsJSON = clamAV.asJSON();
        assertAll(
            () -> assertInstanceOf(Failed.class, scanResult),
            () -> assertTrue(((Failed) scanResult).details().contains("unreachable")),
            () -> assertFalse(clamAV.ping()),
            () -> assertTrue(clamAV.version().isEmpty()),
            () -> assertTrue(statsAsJSON.contains("\"reachable\":false")),
            () -> assertFalse(statsAsJSON.contains("version"))
        );
    }

    @Test
    void mustPingAndTellVersion() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.EICAR_AUTO)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            Optional<String> version = clamAV.version();
            assertAll(
                () -> assertTrue(clamAV.ping()),
                () -> assertTrue(version.orElseThrow().startsWith("ClamAV"))
            );
        }
    }

    @Test
    void mustScanAssetFileAndAsset() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.EICAR_AUTO)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            AssetFile cleanAssetFile = mock(AssetFile.class);
            when(cleanAssetFile.retrieve()).thenReturn(contentOf("Innocent asset content"))
                .thenReturn(contentOf(FakeClamd.EICAR));
            Asset asset = mock(Asset.class);
            when(asset.assetFile()).thenReturn(cleanAssetFile);
            ScanResult assetFileScanResult = clamAV.scan(cleanAssetFile);
            ScanResult assetScanResult = clamAV.scan(asset);
            assertAll(
                () -> assertInstanceOf(Clean.class, assetFileScanResult),
                () -> assertInstanceOf(Infected.class, assetScanResult)
            );
        }
    }

    @Test
    void mustFailWhenAssetFileIsUnreadable() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.EICAR_AUTO)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            AssetFile unreadableAssetFile = mock(AssetFile.class);
            when(unreadableAssetFile.retrieve()).thenReturn(
                new ByteArrayInputStream("Whatever content".getBytes(StandardCharsets.US_ASCII)) {
                    @Override
                    public void close() throws IOException {
                        throw new IOException("This stream cannot be closed");
                    }
                }
            );
            ScanResult scanResult = clamAV.scan(unreadableAssetFile);
            assertAll(
                () -> assertInstanceOf(Failed.class, scanResult),
                () -> assertTrue(((Failed) scanResult).details().contains("Unable to read the scanned content"))
            );
        }
    }

    @Test
    @SneakyThrows
    void mustCountScans() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.EICAR_AUTO)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            clamAV.scan(contentOf("First innocent content"));
            clamAV.scan(contentOf("Second innocent content"));
            clamAV.scan(contentOf(FakeClamd.EICAR));
            fakeClamd.close();
            clamAV.scan(contentOf("Content that cannot be scanned anymore"));
            assertAll(
                () -> assertEquals(4, clamAV.numOfScans()),
                () -> assertEquals(2, clamAV.numOfCleanScans()),
                () -> assertEquals(1, clamAV.numOfInfectedScans()),
                () -> assertEquals(1, clamAV.numOfFailedScans())
            );
        }
    }

    @Test
    void mustServeAsRocketStats() {
        try (FakeClamd fakeClamd = new FakeClamd(FakeClamd.Mode.EICAR_AUTO)) {
            ClamAV clamAV = clamAVFor(fakeClamd.port());
            clamAV.scan(contentOf("Innocent content"));
            clamAV.scan(contentOf(FakeClamd.EICAR));
            String statsAsJSON = clamAV.asJSON();
            assertAll(
                () -> assertEquals(ClamAV.class.getName(), clamAV.name()),
                () -> assertTrue(statsAsJSON.contains("\"reachable\":true")),
                () -> assertTrue(statsAsJSON.contains("ClamAV 1.5.2")),
                () -> assertTrue(statsAsJSON.contains("\"numOfScans\":2")),
                () -> assertTrue(statsAsJSON.contains("\"numOfCleanScans\":1")),
                () -> assertTrue(statsAsJSON.contains("\"numOfInfectedScans\":1")),
                () -> assertTrue(statsAsJSON.contains("\"numOfFailedScans\":0")),
                () -> assertTrue(statsAsJSON.matches(".*\"since\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\".*"))
            );
        }
    }

    @Test
    void mustReconfigure() {
        try (
            FakeClamd initialFakeClamd = new FakeClamd(FakeClamd.Mode.GARBAGE);
            FakeClamd subsequentFakeClamd = new FakeClamd(FakeClamd.Mode.EICAR_AUTO)
        ) {
            ClamAV clamAV = clamAVFor(initialFakeClamd.port());
            assertFalse(clamAV.ping());
            ClamAVConfig newConfig = mock(ClamAVConfig.class);
            when(newConfig.clamav_host()).thenReturn("localhost");
            when(newConfig.clamav_port()).thenReturn(subsequentFakeClamd.port());
            when(newConfig.clamav_connect$_$timeout()).thenReturn(2_000);
            when(newConfig.clamav_read$_$timeout()).thenReturn(10_000);
            clamAV.configure(newConfig);
            assertTrue(clamAV.ping());
        }
    }
}
