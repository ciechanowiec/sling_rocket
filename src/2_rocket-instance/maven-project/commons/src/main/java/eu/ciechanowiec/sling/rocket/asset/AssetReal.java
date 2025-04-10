package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.BasicReferencable;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import java.io.InputStream;
import java.util.Optional;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.ResourceResolver;

@Slf4j
@ToString
class AssetReal implements Asset {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    AssetReal(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        log.trace("Initialized {}", this);
    }

    AssetReal(WithJCRPath withJCRPath, ResourceAccess resourceAccess) {
        this(withJCRPath.jcrPath(), resourceAccess);
    }

    @Override
    public AssetFile assetFile() {
        return new AssetFile() {

            @Override
            public InputStream retrieve() {
                return ntFile().map(NTFile::assetFile)
                    .map(AssetFile::retrieve)
                    .orElse(InputStream.nullInputStream());
            }

            @Override
            public DataSize size() {
                return ntFile().map(NTFile::assetFile)
                    .map(AssetFile::size)
                    .orElse(new DataSize(NumberUtils.LONG_ZERO, DataUnit.BYTES));
            }
        };
    }

    @Override
    public AssetMetadata assetMetadata() {
        log.trace("Retrieving metadata for {}", this);
        JCRPath metadataJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), METADATA_NODE_NAME);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String metadataJCRPathRaw = metadataJCRPath.get();
            return Optional.ofNullable(resourceResolver.getResource(metadataJCRPathRaw))
                .<AssetMetadata>map(metadataResource -> new ResourceMetadata(metadataResource, resourceAccess))
                .orElse(new EmptyMetadata());
        }
    }

    private Optional<NTFile> ntFile() {
        log.trace("Retrieving file for {}", this);
        JCRPath fileJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), FILE_NODE_NAME);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String fileJCRPathRaw = fileJCRPath.get();
            return Optional.ofNullable(resourceResolver.getResource(fileJCRPathRaw))
                .map(fileResource -> new NTFile(fileResource, resourceAccess));
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

    @Override
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        if (comparedObject instanceof Asset) {
            Referencable comparedAsset = (Referencable) comparedObject;
            return jcrUUID().equals(comparedAsset.jcrUUID());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return jcrUUID().hashCode() * 31;
    }
}
