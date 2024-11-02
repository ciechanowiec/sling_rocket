package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;

import javax.jcr.Node;
import javax.jcr.Repository;

/**
 * Represents a request to save a new {@link Node} in the {@link Repository}.
 * @param <T> type representing the saved {@link Node}
 */
@FunctionalInterface
public interface StagedNode<T> {

    /**
     * <p>
     * Saves a new {@link Node} in the {@link Repository} at the specified {@link TargetJCRPath}.
     * </p>
     * Lacking intermediate {@link Node}s are created automatically.
     * @param targetJCRPath non-occupied {@link TargetJCRPath} where the new {@link Node} should be saved
     * @return an object representing the saved {@link Node}
     * @throws OccupiedJCRPathException if the {@code targetJCRPath} is occupied by some {@link Node}
     *                                  at the moment when the new {@link Node} is attempted to be saved
     *                                  at the same {@link JCRPath}
     */
    T save(TargetJCRPath targetJCRPath);
}
