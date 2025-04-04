package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import eu.ciechanowiec.sling.rocket.jcr.BasicReferencable;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents {@link Node} instances of type {@link Assets#NT_ASSETS}. That can be either a persisted or a
 * hypothetically persisted {@link Node}.
 */
@Slf4j
@ToString
public class Assets implements WithJCRPath, Referencable {

    /**
     * The type name of a {@link Node} that holds as direct children other {@link Node}-s of {@link Asset#NT_ASSET_REAL}
     * and {@link Asset#NT_ASSET_LINK} types.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String NT_ASSETS = "rocket:Assets";

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param resource       the {@link Resource} that will back the constructed object; the type of a {@link Node}
     *                       behind the {@link Resource} must be one of the types supported by the {@link Assets}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     */
    public Assets(Resource resource, ResourceAccess resourceAccess) {
        String resourcePath = resource.getPath();
        this.jcrPath = new TargetJCRPath(resourcePath);
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        log.trace("Initialized {}", this);
    }

    /**
     * Returns a {@link Collection} of {@link Asset}-s held in this {@link Assets} instance.
     *
     * @return a {@link Collection} of {@link Asset}-s held in this {@link Assets} instance
     */
    public Collection<Asset> get() {
        log.trace("Retrieving assets from '{}'", jcrPath);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                .map(Resource::getChildren)
                .map(UnwrappedIteration::new)
                .map(UnwrappedIteration::stream)
                .orElseGet(Stream::empty)
                .<Asset>map(resource -> new UniversalAsset(resource, resourceAccess))
                .toList();
        }
    }

    @Override
    public String jcrUUID() {
        Referencable referencable = new BasicReferencable(this, resourceAccess);
        return referencable.jcrUUID();
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_ASSETS);
    }
}
