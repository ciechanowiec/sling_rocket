package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.StagedNode;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import javax.jcr.Node;
import javax.jcr.Repository;
import java.util.Map;

/**
 * Represents a request to save a new {@link Asset} in the {@link Repository} as a {@link Node} of type
 * {@link Asset#NT_ASSET_LINK}.
 *
 * @param linkedAsset    {@link Asset} to which a new {@link Node} of type {@link Asset#NT_ASSET_LINK} should point to
 * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
 *                       resources
 */
@Slf4j
public record StagedAssetLink(Asset linkedAsset, ResourceAccess resourceAccess) implements StagedNode<Asset> {

    @SneakyThrows
    @Override
    public Asset save(TargetJCRPath targetJCRPath) {
        log.trace("Saving {} to '{}'", this, targetJCRPath);
        targetJCRPath.assertThatJCRPathIsFree(resourceAccess);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String linkedJCRUUID = linkedAsset.jcrUUID();
            String targetJCRPathRaw = targetJCRPath.get();
            Resource assetLinkResource = ResourceUtil.getOrCreateResource(
                resourceResolver, targetJCRPathRaw,
                Map.of(
                    JcrConstants.JCR_PRIMARYTYPE, Asset.NT_ASSET_LINK,
                    Asset.PN_LINKED_ASSET, linkedJCRUUID
                ), null, false
            );
            resourceResolver.commit();
            Asset savedAsset = new UniversalAsset(assetLinkResource, resourceAccess);
            log.debug("Saved: {}", savedAsset);
            return savedAsset;
        }
    }
}
