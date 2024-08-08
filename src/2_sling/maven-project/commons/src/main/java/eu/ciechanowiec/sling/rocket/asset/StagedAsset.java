package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;

import javax.jcr.Item;
import javax.jcr.Repository;

/**
 * Represents a request to save a new {@link Asset} in the {@link Repository}.
 */
@FunctionalInterface
public interface StagedAsset {

    /**
     * Saves a new {@link Asset} in the {@link Repository} at the specified {@link TargetJCRPath}.
     * @param targetJCRPath {@link TargetJCRPath} where the new {@link Asset} should be saved
     * @return an instance of the saved {@link Asset}
     * @throws OccupiedJCRPathException if the {@code targetJCRPath} is occupied by some {@link Item}
     *                                  at the moment when the {@link Asset} is attempted to be saved
     *                                  at the same {@link JCRPath}
     */
    Asset save(TargetJCRPath targetJCRPath);
}
