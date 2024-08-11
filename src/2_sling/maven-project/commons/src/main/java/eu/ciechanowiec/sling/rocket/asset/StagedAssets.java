package eu.ciechanowiec.sling.rocket.asset;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import javax.jcr.Item;
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
public record StagedAssets(Collection<StagedAsset> assetsToSave, ResourceAccess resourceAccess) {

    /**
     * <p>
     * Saves a new {@link Assets} instance in the {@link Repository} at the specified {@link TargetJCRPath}.
     * </p>
     * If the {@link StagedAssets#assetsToSave} is empty, the wrapping {@link Node} of type {@link Assets#NT_ASSETS}
     * will still be created, but without any {@link Asset}-s saved within it.
     * @param targetJCRPath {@link TargetJCRPath} where the new {@link Assets} instance should be saved
     * @return an instance of the saved {@link Assets}
     * @throws OccupiedJCRPathException if the {@code targetJCRPath} is occupied by some {@link Item}
     *                                  at the moment when the {@link Assets} instance is attempted to be saved
     *                                  at the same {@link JCRPath}
     */
    @SneakyThrows
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
                           .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Assets.class)))
                           .orElseThrow();
        }
    }
}
