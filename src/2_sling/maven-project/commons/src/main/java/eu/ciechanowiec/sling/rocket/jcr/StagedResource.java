package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;

import javax.jcr.Item;
import javax.jcr.Repository;

/**
 * Represents a request to save a new resource in the {@link Repository}.
 * @param <T> type representing the saved resource
 */
@FunctionalInterface
public interface StagedResource<T> {

    /**
     * Saves a new resource in the {@link Repository} at the specified {@link TargetJCRPath}.
     * @param targetJCRPath {@link TargetJCRPath} where the new resource should be saved
     * @return an object representing the saved resource
     * @throws OccupiedJCRPathException if the {@code targetJCRPath} is occupied by some {@link Item}
     *                                  at the moment when the new resource is attempted to be saved
     *                                  at the same {@link JCRPath}
     */
    T save(TargetJCRPath targetJCRPath);
}
