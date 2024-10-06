package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.BasicReferencable;
import eu.ciechanowiec.sling.rocket.jcr.NTFile;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Optional;

@Slf4j
@ToString
class AssetReal implements Asset {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    AssetReal(Resource resource, ResourceAccess resourceAccess) {
        String resourcePath = resource.getPath();
        this.jcrPath = new TargetJCRPath(resourcePath);
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        log.trace("Initialized {}", this);
    }

    @Override
    public AssetFile assetFile() {
        return () -> file().retrieve();
    }

    @Override
    public AssetMetadata assetMetadata() {
        log.trace("Retrieving metadata for {}", this);
        JCRPath metadataJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), METADATA_NODE_NAME);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String metadataJCRPathRaw = metadataJCRPath.get();
            return Optional.ofNullable(resourceResolver.getResource(metadataJCRPathRaw))
                    .map(metadataResource -> new ResourceMetadata(metadataResource, resourceAccess))
                    .orElseThrow();
        }
    }

    private NTFile file() {
        log.trace("Retrieving file for {}", this);
        JCRPath fileJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), FILE_NODE_NAME);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String fileJCRPathRaw = fileJCRPath.get();
            return Optional.ofNullable(resourceResolver.getResource(fileJCRPathRaw))
                           .map(fileResource -> new NTFile(fileResource, resourceAccess))
                           .orElseThrow();
        }
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_ASSET_REAL);
    }

    @Override
    public String jcrUUID() {
        Referencable referencable = new BasicReferencable(this, resourceAccess);
        return referencable.jcrUUID();
    }
}