package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import java.io.File;
import java.util.Optional;

/**
 * Unary binary file of an {@link Asset}.
 */
@FunctionalInterface
public interface AssetFile {

    /**
     * Name of a {@link Property} of type {@link PropertyType#STRING} that contains the original name of the
     * {@link AssetFile} that was given to it in the source system from which this {@link AssetFile} was obtained, e.g.
     * from the user's file system.
     */
    String PN_ORIGINAL_NAME = "originalName";

    /**
     * Returns an {@link Optional} containing the unary binary file of an {@link Asset}. The wrapped {@link File} is
     * written in a temporary directory of the host operational system.
     *
     * @return {@link Optional} containing the unary binary file of an {@link Asset}; an empty {@link Optional} is
     * returned if due to any reason file retrieve fails
     */
    Optional<File> retrieve();

    /**
     * Returns the {@link DataSize} of the unary binary file of an {@link Asset}.
     *
     * @return {@link DataSize} of the unary binary file of an {@link Asset}
     */
    default DataSize size() {
        return retrieve().map(file -> new DataSize(() -> file)).orElse(new DataSize(0, DataUnit.BYTES));
    }
}
