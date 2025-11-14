package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceRanking;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Default implementation of {@link AssetsAPIDocumentation}.
 */
@Component(
    service = {AssetsAPIDocumentation.class, AssetsAPIDocumentationDefault.class},
    immediate = true
)
@ServiceDescription("Default implementation of AssetsAPIDocumentation")
@ServiceRanking(Integer.MIN_VALUE)
@Slf4j
public class AssetsAPIDocumentationDefault implements AssetsAPIDocumentation {

    /**
     * Source of {@link AssetsAPIDocumentation#html()}.
     */
    private final MemoizingSupplier<String> htmlToSend;

    /**
     * Constructs an instance of this class.
     */
    public AssetsAPIDocumentationDefault() {
        htmlToSend = new MemoizingSupplier<>(this::readHTMLFromFile);
    }

    @SneakyThrows
    private String readHTMLFromFile() {
        String fileName = "assets-api-doc.html";
        File createdFile = File.createTempFile(fileName, ".tmp");
        createdFile.deleteOnExit();
        Path tempFilePath = createdFile.toPath();
        ClassLoader classLoader = ServletDefault.class.getClassLoader();
        log.debug("Reading HTML content from '{}' with {}", fileName, classLoader);
        try (
            InputStream inputStream = Optional.ofNullable(classLoader.getResourceAsStream(fileName)).orElseThrow();
            OutputStream outputStream = Files.newOutputStream(tempFilePath)
        ) {
            IOUtils.copy(inputStream, outputStream);
        }
        return Files.readString(tempFilePath);
    }

    @Override
    @SneakyThrows
    public String html() {
        return htmlToSend.get();
    }
}
