package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import javax.jcr.Node;
import javax.jcr.Repository;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a request to save a new {@link Asset} in the {@link Repository}
 * as a {@link Node} of type {@link Asset#NT_ASSET_REAL}.
 *
 * @param assetFile      {@link AssetFile} to be saved in the {@link Repository}
 * @param simpleMetadata {@link SimpleMetadata} describing the {@link Asset} and to be saved along
 *                       with that {@link Asset} in the {@link Repository}
 * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
 *                       object to acquire access to resources
 */
@Slf4j
public record StagedAssetReal(
        AssetFile assetFile, SimpleMetadata simpleMetadata, ResourceAccess resourceAccess
) implements StagedAsset {

    @SneakyThrows
    @Override
    public Asset save(TargetJCRPath targetJCRPath) {
        log.trace("Saving {} to {}", this, targetJCRPath);
        targetJCRPath.assertThatJCRPathIsFree(resourceAccess);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String assetRealJCRPathRaw = targetJCRPath.get();
            Resource assetRealResource = ResourceUtil.getOrCreateResource(
                    resourceResolver, assetRealJCRPathRaw,
                    Map.of(JcrConstants.JCR_PRIMARYTYPE, Asset.NT_ASSET_REAL), null, false
            );
            log.trace("While saving {} to {}, this resource was staged: {}", this, targetJCRPath, assetRealResource);
            attachFile(assetRealResource, assetFile, simpleMetadata);
            attachMetadata(assetRealResource, simpleMetadata);
            resourceResolver.commit();
            Asset savedAsset = Optional.ofNullable(assetRealResource.adaptTo(Asset.class)).orElseThrow();
            log.debug("Saved: {}", savedAsset);
            return savedAsset;
        }
    }

    @SneakyThrows
    private void attachFile(Resource assetRealResource, AssetFile assetFile, SimpleMetadata simpleMetadata) {
        log.trace("Attaching {} to {}", assetFile, assetRealResource);
        Node assetRealNode = Optional.ofNullable(assetRealResource.adaptTo(Node.class)).orElseThrow();
        String mimeType = simpleMetadata.mimeType();
        File assetFileUnwrapped = assetFile.retrieve().orElseThrow();
        Path pathToAssetFileUnwrapped = assetFileUnwrapped.toPath();
        try (InputStream assetFileIS = Files.newInputStream(pathToAssetFileUnwrapped)) {
            Node assetFileNode = JcrUtils.putFile(assetRealNode, Asset.FILE_NODE_NAME, mimeType, assetFileIS);
            log.trace("Staged for saving: {}", assetFileNode);
        }
    }

    @SneakyThrows
    private void attachMetadata(Resource assetRealResource, SimpleMetadata simpleMetadata) {
        log.trace("Attaching {} to {}", simpleMetadata, assetRealResource);
        String assetRealJCRPathRaw = assetRealResource.getPath();
        JCRPath metadataJCRPath = new TargetJCRPath(new ParentJCRPath(
                new TargetJCRPath(assetRealJCRPathRaw)), Asset.METADATA_NODE_NAME
        );
        String metadataJCRPathRaw = metadataJCRPath.get();
        SimpleMetadata simpleMetadataWithNodeType = simpleMetadata.append(
                JcrConstants.JCR_PRIMARYTYPE, Asset.NT_ASSET_METADATA
        );
        @SuppressWarnings("PMD.LongVariable")
        Map<String, Object> simpleMetadataWithNodeTypeUnwrapped = simpleMetadataWithNodeType.allButObjectValues();
        @SuppressWarnings("PMD.CloseResource")
        ResourceResolver resourceResolver = assetRealResource.getResourceResolver();
        Resource metadataResource = ResourceUtil.getOrCreateResource(
                resourceResolver, metadataJCRPathRaw,
                simpleMetadataWithNodeTypeUnwrapped, null, false
        );
        log.trace("Staged for saving {}", metadataResource);
    }
}
