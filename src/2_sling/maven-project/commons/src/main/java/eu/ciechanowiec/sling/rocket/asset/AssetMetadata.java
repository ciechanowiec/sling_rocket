package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;

import javax.jcr.Node;

/**
 * Represents a {@link Node} that holds metadata of an {@link Asset}.
 */
@FunctionalInterface
public interface AssetMetadata {

    /**
     * Returns properties of the underlying {@link Node}.
     * @return properties of the underlying {@link Node}
     */
    NodeProperties retrieve();
}
