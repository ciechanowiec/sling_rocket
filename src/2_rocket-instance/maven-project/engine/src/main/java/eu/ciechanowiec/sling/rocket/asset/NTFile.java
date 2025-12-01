package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.*;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.ref.Referenceable;
import eu.ciechanowiec.sling.rocket.jcr.ref.ReferenceableSimple;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import jakarta.ws.rs.core.MediaType;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

@Slf4j
@ToString
class NTFile implements Asset {

    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    NTFile(Resource resource, ResourceAccess resourceAccess) {
        this(new TargetJCRPath(resource), resourceAccess);
    }

    NTFile(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        assertContentChildNodeType();
        log.trace("Initialized {}", this);
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
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        NodeProperties jcrContentChildNP = new NodeProperties(jcrContentChildJCRPath, resourceAccess);
        return new AssetFile() {

            @Override
            public InputStream retrieve() {
                return jcrContentChildNP.retrieveBinary(JcrConstants.JCR_DATA);
            }

            @Override
            @SuppressWarnings("PMD.LinguisticNaming")
            public DataSize size() {
                try (InputStreamWithDataSize isWithDataSize = jcrContentChildNP.retrieveBinary(JcrConstants.JCR_DATA)) {
                    return isWithDataSize.dataSize();
                }
            }
        };
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
        Referenceable jcrContentChildReferenceable = new ReferenceableSimple(
            () -> jcrContentChildJCRPath, resourceAccess
        );
        return jcrContentChildReferenceable.jcrUUID();
    }

    @Override
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        if (comparedObject instanceof Asset) {
            Referenceable comparedAsset = (Referenceable) comparedObject;
            return jcrUUID().equals(comparedAsset.jcrUUID());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return jcrUUID().hashCode() * 31;
    }

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }
}
