package eu.ciechanowiec.sling.rocket.asset.image;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"ClassWithTooManyFields", "PMD.TooManyFields"})
class ComparableImagesTest {

    private File a1;
    private File a2;
    private File a3;
    private File a4;
    private File b1;
    private File b2;
    private File b3;
    private File b4;
    private File mp3;

    @SuppressWarnings("ExtractMethodRecommender")
    @Test
    void mustExcludeSimilarImages() {
        ComparableImages comparableImages = new ComparableImages(List.of(
            new ComparableImage(a1),
            new ComparableImage(a2),
            new ComparableImage(a3),
            new ComparableImage(a4),
            new ComparableImage(b1),
            new ComparableImage(b2),
            new ComparableImage(b3),
            new ComparableImage(b4),
            new ComparableImage(mp3)
        ));
        ComparableImages uniqueImages = comparableImages.excludeSimilarImages();
        Collection<String> namesOfFiles = uniqueImages.asFiles().stream().map(File::getName).toList();
        int numOfFiles = namesOfFiles.size();
        assertAll(
            () -> assertEquals(5, numOfFiles),
            () -> assertTrue(namesOfFiles.stream().anyMatch(name -> name.contains("_a-3_"))),
            () -> assertTrue(namesOfFiles.stream().anyMatch(name -> name.contains("_a-4_"))),
            () -> assertTrue(namesOfFiles.stream().anyMatch(name -> name.contains("_b-3_"))),
            () -> assertTrue(namesOfFiles.stream().anyMatch(name -> name.contains("_b-4_"))),
            () -> assertTrue(namesOfFiles.stream().anyMatch(name -> name.contains("_time-forward_")))
        );
    }

    @SneakyThrows
    @BeforeEach
    void setup() {
        a1 = loadResourceIntoFile("image_sets/a-1.jpg", "a-1");
        a2 = loadResourceIntoFile("image_sets/a-2.jpg", "a-2");
        a3 = loadResourceIntoFile("image_sets/a-3.jpg", "a-3");
        a4 = loadResourceIntoFile("image_sets/a-4.jpg", "a-4");
        b1 = loadResourceIntoFile("image_sets/b-1.jpg", "b-1");
        b2 = loadResourceIntoFile("image_sets/b-2.jpg", "b-2");
        b3 = loadResourceIntoFile("image_sets/b-3.jpg", "b-3");
        b4 = loadResourceIntoFile("image_sets/b-4.jpg", "b-4");
        mp3 = loadResourceIntoFile("time-forward.mp3", "time-forward");
    }

    @SneakyThrows
    private File loadResourceIntoFile(String resourceName, String fileName) {
        File createdFile = File.createTempFile("jcr-binary_" + fileName + "_", ".tmp");
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
}
