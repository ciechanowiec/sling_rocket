package eu.ciechanowiec.sling.rocket.asset;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NTFile;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Node;
import javax.jcr.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link Asset} that can be constructed from any {@link Resource} that
 * represents an {@link Asset} in the {@link Repository}.
 */
@ToString
@Slf4j
public class UniversalAsset implements Asset {

    private final Asset source;

    /**
     * Constructs an instance of this class.
     * @param resource the {@link Resource} that will back the constructed object; the type of a {@link Node}
     *                 behind the {@link Resource} must be one of the types supported by the {@link Asset}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                       object to acquire access to resources
     * @throws IllegalArgumentException if the primary type of the {@link Node} behind the {@link Resource}
     *                                  is not supported by the {@link Asset}
     */
    @SuppressWarnings("WeakerAccess")
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public UniversalAsset(Resource resource, ResourceAccess resourceAccess) {
        String resourcePath = resource.getPath();
        NodeProperties nodeProperties = new NodeProperties(
                new TargetJCRPath(resourcePath), resourceAccess
        );
        Map<String, Supplier<? extends Asset>> implementationMappings = Map.of(
                NT_ASSET_REAL, () -> new AssetReal(resource, resourceAccess),
                NT_ASSET_LINK, () -> new AssetLink(resource, resourceAccess),
                JcrConstants.NT_FILE, () -> new NTFile(resource, resourceAccess),
                JcrConstants.NT_RESOURCE, () -> new NTResource(resource, resourceAccess)
        );
        String primaryType = nodeProperties.primaryType();
        source = Optional.ofNullable(implementationMappings.get(primaryType))
                .map(Supplier::get)
                .orElseThrow(
                        () -> {
                            String message = String.format("Unsupported primary type of %s: %s", resource, primaryType);
                            return new IllegalArgumentException(message);
                        }
                );
        log.trace("Initialized {}", this);
    }

    @Override
    public AssetFile assetFile() {
        return source.assetFile();
    }

    @Override
    public AssetMetadata assetMetadata() {
        return source.assetMetadata();
    }

    @Override
    public String jcrUUID() {
        return source.jcrUUID();
    }

    @Override
    public JCRPath jcrPath() {
        return source.jcrPath();
    }
}
