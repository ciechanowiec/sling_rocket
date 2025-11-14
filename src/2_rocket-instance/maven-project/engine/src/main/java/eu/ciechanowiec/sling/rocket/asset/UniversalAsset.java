package eu.ciechanowiec.sling.rocket.asset;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.IllegalPrimaryTypeException;
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
 * {@link Asset} that can be constructed from any {@link Resource} or {@link JCRPath} that points to an {@link Asset} in
 * the {@link Repository}.
 */
@ToString
@Slf4j
public class UniversalAsset implements Asset {

    private final Asset source;

    /**
     * Constructs an instance of this class.
     *
     * @param resource       the {@link Resource} that will back the constructed object; the type of a {@link Node}
     *                       behind the {@link Resource} must be one of the types supported by the {@link Asset}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} behind the {@link Resource} is not
     *                                     supported by the {@link Asset}
     */
    @SuppressWarnings("WeakerAccess")
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public UniversalAsset(Resource resource, ResourceAccess resourceAccess) {
        this(new TargetJCRPath(resource), resourceAccess);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param jcrPath        the {@link JCRPath} to the {@link Node} that will back the constructed object; the type of
     *                       the {@link Node} must be one of the types supported by the {@link Asset}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} is not supported by the
     *                                     {@link Asset}
     */
    @SuppressWarnings("WeakerAccess")
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public UniversalAsset(JCRPath jcrPath, ResourceAccess resourceAccess) {
        NodeProperties nodeProperties = new NodeProperties(
            jcrPath, resourceAccess
        );
        Map<String, Supplier<? extends Asset>> implementationMappings = Map.of(
            NT_ASSET_REAL, () -> new AssetReal(jcrPath, resourceAccess),
            NT_ASSET_LINK, () -> new AssetLink(jcrPath, resourceAccess),
            JcrConstants.NT_FILE, () -> new AssetRealCape(new NTFile(jcrPath, resourceAccess), resourceAccess),
            JcrConstants.NT_RESOURCE, () -> new AssetRealCape(
                new NTFile(
                    new JCRPathWithParent(jcrPath, resourceAccess
                    ).parent().orElseThrow(), resourceAccess
                ), resourceAccess
            )
        );
        String primaryType = nodeProperties.primaryType();
        source = Optional.ofNullable(implementationMappings.get(primaryType))
            .map(Supplier::get)
            .orElseThrow(
                () -> {
                    String message = String.format("Unsupported primary type of %s: %s", jcrPath, primaryType);
                    return new IllegalPrimaryTypeException(message);
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

    @Override
    public boolean equals(Object comparedObject) {
        return source.equals(comparedObject);
    }

    @Override
    public int hashCode() {
        return jcrUUID().hashCode() * 31;
    }
}
