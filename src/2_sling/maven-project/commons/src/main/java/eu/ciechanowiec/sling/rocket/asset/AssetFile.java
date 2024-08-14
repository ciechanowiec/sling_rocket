package eu.ciechanowiec.sling.rocket.asset;

import java.io.File;
import java.util.Optional;

/**
 * Unary binary file of an {@link Asset}.
 */
@FunctionalInterface
public interface AssetFile {

    /**
     * Returns an {@link Optional} containing the unary binary file of an {@link Asset}.
     * @return {@link Optional} containing the unary binary file of an {@link Asset};
     *         an empty {@link Optional} is returned if due to any reason file retrieve fails
     */
    Optional<File> retrieve();
}
