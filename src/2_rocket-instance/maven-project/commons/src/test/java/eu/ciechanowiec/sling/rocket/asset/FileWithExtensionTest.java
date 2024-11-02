package eu.ciechanowiec.sling.rocket.asset;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("MultipleStringLiterals")
class FileWithExtensionTest {

    @TempDir
    private Path tempDir;

    private FileWithExtension fileWithExtension;

    @BeforeEach
    void setup() throws IOException {
        File file = new File(tempDir.toFile(), "testFile");
        assertTrue(file.createNewFile());
        fileWithExtension = new FileWithExtension(file);
    }

    @Test
    void shouldChangeFileNameBasedOnMimeType() {
        String newBasicName = "renamedFile";
        String mimeTypeName = "image/jpeg";

        File renamedFile = fileWithExtension.rename(newBasicName, mimeTypeName);

        assertAll(
                () -> assertEquals("renamedFile.jpg", renamedFile.getName()),
                () -> assertTrue(renamedFile.exists())
        );
    }

    @Test
    void shouldNotChangeFileNameIfMimeTypeIsInvalid() {
        String newBasicName = "renamedFile";
        String invalidMimeTypeName = "invalid/type";

        File renamedFile = fileWithExtension.rename(newBasicName, invalidMimeTypeName);

        assertAll(
                () -> assertEquals("testFile", renamedFile.getName()),
                () -> assertTrue(renamedFile.exists())
        );
    }

    @Test
    void shouldReturnOriginalFileWhenRenamingFails() {
        File mockFile = mock(File.class);
        when(mockFile.renameTo(any(File.class))).thenReturn(false);
        FileWithExtension fileWithMock = new FileWithExtension(mockFile);

        String newBasicName = "renamedFile";
        String mimeTypeName = "image/png";

        File resultFile = fileWithMock.rename(newBasicName, mimeTypeName);

        assertAll(
                () -> assertSame(mockFile, resultFile),
                () -> verify(mockFile).renameTo(any(File.class))
        );
    }

    @SneakyThrows
    @Test
    void shouldHandleNullParentDirectoryGracefully() {
        File originalFileWithoutParent = new File("noParentFile");
        assertTrue(originalFileWithoutParent.createNewFile());
        FileWithExtension fileWithoutParent = new FileWithExtension(originalFileWithoutParent);

        String newBasicName = "fileWithoutParent";
        String mimeTypeName = "text/plain";

        File renamedFile = fileWithoutParent.rename(newBasicName, mimeTypeName);

        assertEquals("fileWithoutParent.txt", renamedFile.getName());
    }
}
