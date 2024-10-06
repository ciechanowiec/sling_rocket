package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * In-memory {@link AssetMetadata} generated for a {@link File}. The mime type is detected automatically.
 */
@Slf4j
@ToString
public class FileMetadata implements AssetMetadata {

    @ToString.Exclude
    private final Supplier<String> mimeTypeSupplier;
    @ToString.Exclude
    private final Supplier<Map<String, String>> allSupplier;
    @ToString.Exclude
    private final Supplier<Optional<NodeProperties>> propertiesSupplier;

    /**
     * Constructs an instance of this class.
     * @param file {@link File} for which this {@link FileMetadata} will be generated
     */
    @SuppressWarnings("WeakerAccess")
    public FileMetadata(File file) {
        mimeTypeSupplier = SneakySupplier.sneaky(() -> {
            Tika tika = new Tika();
            log.trace("Detecting the mime type of {}", file);
            String detectedMimeType = tika.detect(file);
            log.trace("Mime type for {} detected: {}", file, detectedMimeType);
            return detectedMimeType;
        });
        allSupplier = () -> Map.of(PN_MIME_TYPE, mimeType());
        propertiesSupplier = Optional::empty;
        log.trace("Initialized {} out of this file: {}", this, file);
    }

    @Override
    public String mimeType() {
        return mimeTypeSupplier.get();
    }

    @Override
    public Map<String, String> all() {
        return allSupplier.get();
    }

    @Override
    public Optional<NodeProperties> properties() {
        return propertiesSupplier.get();
    }
}