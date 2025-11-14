package eu.ciechanowiec.sling.rocket.jcr;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.asset.UsualFileAsAssetFile;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class InputStreamWithDataSizeTest extends TestEnvironment {

    private File file;
    private TargetJCRPath realAssetPath;
    private JCRPath jcrContent;

    InputStreamWithDataSizeTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        file = loadResourceIntoFile("1.jpeg");
        realAssetPath = new TargetJCRPath("/content/dam/rocket/1.jpeg");
        jcrContent = new TargetJCRPath(
            new ParentJCRPath(new TargetJCRPath(new ParentJCRPath(realAssetPath), Asset.FILE_NODE_NAME)),
            JcrConstants.JCR_CONTENT
        );
    }

    private void saveFileInJCR() {
        new StagedAssetReal(new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess).save(
            realAssetPath);
    }

    @Test
    @SneakyThrows
    void readsAllBytesFromJCRCorrectly() {
        saveFileInJCR();
        byte[] expected = Files.readAllBytes(file.toPath());

        try (
            InputStreamWithDataSize inputStreamRA =
                new InputStreamWithDataSize(jcrContent, JcrConstants.JCR_DATA, fullResourceAccess);
            InputStreamWithDataSize inputStreamRR =
                new InputStreamWithDataSize(jcrContent, JcrConstants.JCR_DATA, context.resourceResolver())
        ) {
            byte[] actualRAFirstRead = inputStreamRA.readAllBytes();
            byte[] actualRASecondRead = inputStreamRA.readAllBytes();
            byte[] actualRR = inputStreamRR.readAllBytes();
            assertAll(
                () -> assertArrayEquals(
                    expected, actualRAFirstRead, "File content read from JCR should match the original file"
                ),
                () -> assertEquals(0, actualRASecondRead.length),
                () -> assertArrayEquals(expected, actualRR, "File content read from JCR should match the original file")
            );
        }
    }

    @Test
    @SneakyThrows
    void transferToCopiesAllData() {
        saveFileInJCR();
        byte[] expected = Files.readAllBytes(file.toPath());

        try (
            InputStreamWithDataSize inputStream =
                new InputStreamWithDataSize(jcrContent, JcrConstants.JCR_DATA, fullResourceAccess);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {

            long bytesTransferred = inputStream.transferTo(outputStream);
            assertEquals(expected.length, bytesTransferred, "Number of bytes transferred should match the file size");
            assertArrayEquals(expected, outputStream.toByteArray(), "Transferred bytes should match original file");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    @SneakyThrows
    @SuppressFBWarnings("RR_NOT_CHECKED")
    void markAndResetWorksIfSupported() {
        saveFileInJCR();
        byte[] expected = Files.readAllBytes(file.toPath());

        try (
            InputStreamWithDataSize inputStream =
                new InputStreamWithDataSize(jcrContent, JcrConstants.JCR_DATA, fullResourceAccess)
        ) {

            if (inputStream.markSupported()) {
                // Mark at the very beginning
                inputStream.mark(expected.length + 100); // big read limit just to be safe

                // Read some partial data
                byte[] partial = new byte[100];
                int partialRead = inputStream.read(partial);
                assertTrue(partialRead > 0);

                // Read some more
                @SuppressWarnings("MagicNumber")
                byte[] more = new byte[200];
                inputStream.read(more);

                // Now reset to the mark (which is at the start)
                inputStream.reset();

                // Read the entire file
                byte[] allBytes = inputStream.readAllBytes();
                assertArrayEquals(
                    expected, allBytes,
                    "After reset to beginning, reading the stream should match the original file"
                );
            } else {
                // If not supported, no need to test further
                assertDoesNotThrow(inputStream::markSupported, "mark/reset not supported here");
            }
        }
    }

    @SneakyThrows
    @Test
    void readNBytesWorks() {
        saveFileInJCR();
        byte[] expected = Files.readAllBytes(file.toPath());

        try (
            InputStreamWithDataSize inputStream =
                new InputStreamWithDataSize(jcrContent, JcrConstants.JCR_DATA, fullResourceAccess)
        ) {
            // readNBytes – let's grab half the file
            int halfLength = expected.length / 2;
            byte[] halfBuffer = new byte[halfLength];
            int actuallyRead = inputStream.readNBytes(halfBuffer, 0, halfLength);
            assertEquals(halfLength, actuallyRead);

            // readAllBytes – read the remaining
            byte[] remainingBytes = inputStream.readAllBytes();

            // Combine and compare to original
            byte[] combined = new byte[halfLength + remainingBytes.length];
            System.arraycopy(halfBuffer, 0, combined, 0, halfLength);
            System.arraycopy(remainingBytes, 0, combined, halfLength, remainingBytes.length);

            assertArrayEquals(expected, combined, "Combined partial reads should match entire file content");
        }
    }

    @Test
    void handlesMissingBinaryPropertyGracefully() {
        // Note: We do NOT save the file into JCR, so jcr:data won't exist
        try (
            InputStreamWithDataSize inputStream =
                new InputStreamWithDataSize(jcrContent, "jcr:data", fullResourceAccess)
        ) {

            // Should read as empty (no node/binary property found)
            byte[] actual = inputStream.readAllBytes();
            assertEquals(0, actual.length, "No binary property means no bytes should be read");
        }
    }

    @Test
    void closeCanBeCalledMultipleTimesWithoutError() {
        // Even if we never saved data, closing multiple times should not fail
        try (
            InputStreamWithDataSize inputStream = new InputStreamWithDataSize(
                jcrContent, JcrConstants.JCR_DATA, fullResourceAccess
            )
        ) {
            assertAll(
                () -> assertDoesNotThrow(inputStream::close),
                () -> assertDoesNotThrow(inputStream::close)
            );
        }
    }

    @SneakyThrows
    @Test
    void mustNotCloseExternalRR() {
        saveFileInJCR();
        ResourceResolver resourceResolver = context.resourceResolver();
        byte[] expected = Files.readAllBytes(file.toPath());
        try (
            InputStreamWithDataSize inputStream = new InputStreamWithDataSize(
                jcrContent, JcrConstants.JCR_DATA, resourceResolver
            )
        ) {
            byte[] actual = inputStream.readAllBytes();
            assertAll(
                () -> assertDoesNotThrow(inputStream::close),
                () -> assertArrayEquals(expected, actual, "File content read from JCR should match the original file")
            );
        }
        assertTrue(resourceResolver.isLive());
        // Intentionally checking if we can reuse the ResourceResolver
        try (
            InputStreamWithDataSize inputStream = new InputStreamWithDataSize(
                jcrContent, JcrConstants.JCR_DATA, resourceResolver
            )
        ) {
            byte[] actual = inputStream.readAllBytes();
            assertAll(
                () -> assertDoesNotThrow(inputStream::close),
                () -> assertArrayEquals(expected, actual, "File content read from JCR should match the original file")
            );
        }
    }
}
