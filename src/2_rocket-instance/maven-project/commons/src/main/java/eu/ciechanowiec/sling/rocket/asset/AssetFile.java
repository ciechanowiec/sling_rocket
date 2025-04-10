package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.unit.DataSize;
import java.io.InputStream;
import javax.jcr.Property;
import javax.jcr.PropertyType;

/**
 * Unary binary file of an {@link Asset}.
 */
public interface AssetFile {

    /**
     * Name of a {@link Property} of type {@link PropertyType#STRING} that contains the original name of the
     * {@link AssetFile} that was given to it in the source system from which this {@link AssetFile} was obtained, e.g.
     * from the user's file system.
     */
    String PN_ORIGINAL_NAME = "originalName";

    /**
     * Returns the unary binary file of an {@link Asset} represented as an {@link InputStream}.
     *
     * @return the unary binary file of an {@link Asset} represented as an {@link InputStream}
     */
    InputStream retrieve();

    /**
     * Returns the {@link DataSize} of the unary binary file of an {@link Asset}.
     *
     * @return {@link DataSize} of the unary binary file of an {@link Asset}
     */
    DataSize size();
}
