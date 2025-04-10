package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.unit.DataSize;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.SneakyThrows;

/**
 * {@link AssetFile} made out of a {@link File}.
 */
public class UsualFileAsAssetFile implements AssetFile {

    private final File file;

    /**
     * Constructs an instance of this class.
     *
     * @param file {@link File} to be wrapped by the constructed object
     */
    public UsualFileAsAssetFile(File file) {
        this.file = file;
    }

    @SneakyThrows
    @Override
    public InputStream retrieve() {
        return Files.newInputStream(file.toPath());
    }

    @Override
    public DataSize size() {
        return new DataSize(file);
    }
}
