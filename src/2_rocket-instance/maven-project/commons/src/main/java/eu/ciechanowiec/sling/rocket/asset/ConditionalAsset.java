package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import java.util.Optional;

/**
 * Represent a {@link Node} that might be an {@link Asset}.
 */
public class ConditionalAsset {

    private final JCRPath jcrPath;
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     * @param jcrPath {@link JCRPath} that points to the {@link Node} that might be an {@link Asset}
     * @param resourceAccess {@link ResourceAccess} that will be used to acquire access to resources
     */
    @SuppressWarnings("WeakerAccess")
    public ConditionalAsset(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
    }

    /**
     * Returns an {@link Optional} containing an {@link Asset} built from the {@link Node} represented by this
     * {@link ConditionalAsset} if that {@link Node} exists and is of one of primary types specified in
     * {@link Asset#SUPPORTED_PRIMARY_TYPES}. Returns an empty {@link Optional} otherwise.
     * @return {@link Optional} containing an {@link Asset} built from the {@link Node} represented by this
     *         {@link ConditionalAsset} if that {@link Node} exists and is of one of primary types specified in
     *         {@link Asset#SUPPORTED_PRIMARY_TYPES}; returns an empty {@link Optional} otherwise
     */
    public Optional<Asset> get() {
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                    .filter(
                            resource -> new NodeProperties(
                                    new TargetJCRPath(resource), resourceAccess
                            ).isPrimaryType(Asset.SUPPORTED_PRIMARY_TYPES)
                    )
                    .map(resource -> new UniversalAsset(resource, resourceAccess));
        }
    }
}
