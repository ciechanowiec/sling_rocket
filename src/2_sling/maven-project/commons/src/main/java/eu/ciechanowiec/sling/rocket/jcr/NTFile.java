package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetFile;
import eu.ciechanowiec.sling.rocket.asset.AssetMetadata;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import jakarta.ws.rs.core.MediaType;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Node;
import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 * Represents {@link Node} instances of type {@link JcrConstants#NT_FILE}.
 */
@Slf4j
@ToString
public class NTFile implements Asset {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     * @param resource the {@link Resource} that will back the constructed object; the type of a {@link Node}
     *                 behind the {@link Resource} must be one of the types supported by the {@link NTFile}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                       object to acquire access to resources
     */
    public NTFile(Resource resource, ResourceAccess resourceAccess) {
        this(new TargetJCRPath(resource), resourceAccess);
    }

    /**
     * Constructs an instance of this class.
     * @param jcrPath the {@link JCRPath} to the {@link Node} that will back the constructed object;
     *                the type of the {@link Node} must be one of the types supported by the {@link NTFile}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                       object to acquire access to resources
     */
    public NTFile(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        assertContentChildNodeType();
        log.trace("Initialized {}", this);
    }

    /**
     * Returns an {@link Optional} containing the binary file stored in the underlying {@link Node}.
     * @return {@link Optional} containing the binary file stored in the underlying {@link Node};
     *         empty {@link Optional} is returned if the file cannot be retrieved
     */
    public Optional<File> retrieve() {
        log.trace("Retrieving a file from {}", this);
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        NodeProperties jcrContentChildNP = new NodeProperties(jcrContentChildJCRPath, resourceAccess);
        return jcrContentChildNP.retrieveFile(JcrConstants.JCR_DATA);
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(JcrConstants.NT_FILE);
    }

    private void assertContentChildNodeType() {
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        log.trace("Asserting primary type of {}", jcrContentChildJCRPath);
        NodeProperties jcrContentChildNP = new NodeProperties(jcrContentChildJCRPath, resourceAccess);
        jcrContentChildNP.assertPrimaryType(JcrConstants.NT_RESOURCE);
    }

    @Override
    public AssetFile assetFile() {
        return this::retrieve;
    }

    @Override
    public AssetMetadata assetMetadata() {
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        NodeProperties jcrContentChildNP = new NodeProperties(jcrContentChildJCRPath, resourceAccess);
        return new AssetMetadata() {
            @Override
            public String mimeType() {
                return properties().flatMap(
                        nodeProperties -> nodeProperties.propertyValue(
                                JcrConstants.JCR_MIMETYPE, DefaultProperties.STRING_CLASS
                        )
                ).orElse(MediaType.WILDCARD);
            }

            @Override
            public Map<String, String> all() {
                return properties().map(NodeProperties::all).orElse(Map.of());
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.of(jcrContentChildNP);
            }
        };
    }

    @Override
    public String jcrUUID() {
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        Referencable referencable = new BasicReferencable(() -> jcrContentChildJCRPath, resourceAccess);
        return referencable.jcrUUID();
    }
}
