package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NTFile;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Optional;

@Slf4j
@ToString
class NTResource implements Asset {

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    NTResource(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        assertParentNodeType();
        log.trace("Initialized {}", this);
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(jcrPath, resourceAccess);
        nodeProperties.assertPrimaryType(JcrConstants.NT_RESOURCE);
    }

    private void assertParentNodeType() {
        ParentJCRPath parentJCRPath = parentPath();
        log.trace("Asserting primary type of {}", parentJCRPath);
        NodeProperties parentNP = new NodeProperties(parentJCRPath, resourceAccess);
        parentNP.assertPrimaryType(JcrConstants.NT_FILE);
    }

    private ParentJCRPath parentPath() {
        return new JCRPathWithParent(jcrPath, resourceAccess).parent().orElseThrow();
    }

    private Asset parentAsset() {
        ParentJCRPath parentJCRPath = parentPath();
        log.trace("Parent JCR path for {} is {}", this, parentJCRPath);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String parentJCRPathRaw = parentJCRPath.get();
            return Optional.ofNullable(resourceResolver.getResource(parentJCRPathRaw))
                           .map(resource -> new NTFile(resource, resourceAccess))
                           .orElseThrow();
        }
    }

    @Override
    public AssetFile assetFile() {
        return parentAsset().assetFile();
    }

    @Override
    public AssetMetadata assetMetadata() {
        return parentAsset().assetMetadata();
    }

    @Override
    public String jcrUUID() {
        return parentAsset().jcrUUID();
    }

    @Override
    public JCRPath jcrPath() {
        return parentAsset().jcrPath();
    }
}
