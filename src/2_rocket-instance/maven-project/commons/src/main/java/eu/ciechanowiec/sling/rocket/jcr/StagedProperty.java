package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;

/**
 * Represents a request to save a new {@link Property} in the {@link Repository}.
 *
 * @param <T> type representing the saved {@link Property}
 */
@FunctionalInterface
public interface StagedProperty<T> {

    /**
     * <p>
     * Saves a new {@link Property} in the {@link Repository}. The {@link Property} is saved on an existing {@link Node}
     * specified by the {@code nodeJCRPath} parameter.
     * </p>
     *
     * @param nodeJCRPath {@link JCRPath} to the existing {@link Node} on which the new {@link Property} should be
     *                    saved
     * @return an object representing the saved {@link Property}
     * @throws OccupiedJCRPathException if the {@link Node} has the {@link Property} with the same name as the
     *                                  {@link Property} which is attempted to be saved
     */
    T save(ParentJCRPath nodeJCRPath);
}
