package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;

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
     *
     * @param file {@link File} for which this {@link FileMetadata} will be generated
     */
    @SuppressWarnings("WeakerAccess")
    public FileMetadata(File file) {
        this(() -> file);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param fileSupplier {@link Supplier} that will provide a {@link File} for which this {@link FileMetadata} will be
     *                     generated
     */
    @SuppressWarnings("WeakerAccess")
    public FileMetadata(Supplier<File> fileSupplier) {
        mimeTypeSupplier = SneakySupplier.sneaky(() -> {
            File file = fileSupplier.get();
            Tika tika = new Tika();
            log.trace("Detecting the mime type of {}", file);
            String detectedMimeType = tika.detect(file);
            log.trace("Mime type for {} detected: {}", file, detectedMimeType);
            return detectedMimeType;
        });
        allSupplier = () -> Map.of(PN_MIME_TYPE, mimeType());
        propertiesSupplier = Optional::empty;
        log.trace("Initialized {}", this);
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
