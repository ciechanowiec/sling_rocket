package eu.ciechanowiec.sling.rocket.asset;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.StagedNode;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import javax.jcr.Node;
import javax.jcr.Repository;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a request to save a new {@link Assets} instance in the {@link Repository}
 * as a {@link Node} of type {@link Assets#NT_ASSETS}.
 *
 * @param assetsToSave {@link Asset}-s to be saved within the request
 * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
 *                       object to acquire access to resources
 */
@SuppressWarnings("WeakerAccess")
@SuppressFBWarnings("EI_EXPOSE_REP")
@Slf4j
public record StagedAssets(
        Collection<StagedNode<Asset>> assetsToSave, ResourceAccess resourceAccess
) implements StagedNode<Assets> {

    @SneakyThrows
    @Override
    public Assets save(TargetJCRPath targetJCRPath) {
        log.trace("Saving {} to {}", assetsToSave, targetJCRPath);
        targetJCRPath.assertThatJCRPathIsFree(resourceAccess);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String assetsJCRPathRaw = targetJCRPath.get();
            Resource targetResource = ResourceUtil.getOrCreateResource(
                    resourceResolver, assetsJCRPathRaw,
                    Map.of(JcrConstants.JCR_PRIMARYTYPE, Assets.NT_ASSETS), null, true
            );
            log.trace("Created {}", targetResource);
        }
        assetsToSave.forEach(
                assetToSave -> assetToSave.save(new TargetJCRPath(new ParentJCRPath(targetJCRPath), UUID.randomUUID()))
        );
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String targetJCRPathRaw = targetJCRPath.get();
            return Optional.ofNullable(resourceResolver.getResource(targetJCRPathRaw))
                           .map(resource -> new Assets(resource, resourceAccess))
                           .orElseThrow();
        }
    }
}
