package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;

import java.io.File;
import java.util.Optional;

/**
 * Unary binary file of an {@link Asset}.
 */
@FunctionalInterface
public interface AssetFile {

    /**
     * Returns an {@link Optional} containing the unary binary file of an {@link Asset}.
     * The wrapped {@link File} is written in a temporary directory of the host operational system.
     * @return {@link Optional} containing the unary binary file of an {@link Asset};
     *         an empty {@link Optional} is returned if due to any reason file retrieve fails
     */
    Optional<File> retrieve();

    /**
     * Returns the {@link DataSize} of the unary binary file of an {@link Asset}.
     * @return {@link DataSize} of the unary binary file of an {@link Asset}
     */
    default DataSize size() {
        return retrieve().map(file -> new DataSize(() -> file)).orElse(new DataSize(0, DataUnit.BYTES));
    }
}
