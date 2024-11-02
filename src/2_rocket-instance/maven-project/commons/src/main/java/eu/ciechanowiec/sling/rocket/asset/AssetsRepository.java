package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.jcr.query.Query;
import java.util.Optional;

/**
 * Repository for {@link Asset}s.
 */
@Component(
        service = AssetsRepository.class,
        immediate = true
)
@Slf4j
@ToString
@ServiceDescription("Repository for Assets")
public class AssetsRepository {

    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     * @param resourceAccess {@link ResourceAccess} that will be used to acquire access to resources
     */
    @Activate
    public AssetsRepository(
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            ResourceAccess resourceAccess
    ) {
        this.resourceAccess = resourceAccess;
        log.info("Initialized {}", this);
    }

    /**
     * Finds an {@link Asset} for the given {@link Referencable}.
     * @param referencable {@link Referencable} pointing to the searched {@link Asset}
     * @return {@link Optional} containing the found {@link Asset};
     *         empty {@link Optional} is returned if no related {@link Asset} was found
     */
    @SuppressWarnings("WeakerAccess")
    public Optional<Asset> find(Referencable referencable) {
        String query = String.format(
                "SELECT * FROM [%s] AS node "
                + "WHERE node.[%s] = '%s' "
                + "AND ("
                + "node.[%s] = '%s' "
                + "OR node.[%s] = '%s' "
                + "OR node.[%s] = '%s' "
                + "OR node.[%s] = '%s'"
                + ")",
                JcrConstants.NT_BASE,
                JcrConstants.JCR_UUID, referencable.jcrUUID(),
                JcrConstants.JCR_PRIMARYTYPE, Asset.NT_ASSET_REAL,
                JcrConstants.JCR_PRIMARYTYPE, Asset.NT_ASSET_LINK,
                JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE,
                JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE
        );
        log.trace("Searching for Asset for {}. UUID: '{}'. Query: {}", referencable, referencable.jcrUUID(), query);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Optional<Asset> assetNullable = new UnwrappedIteration<>(
                    resourceResolver.findResources(query, Query.JCR_SQL2)
            ).stream()
             .findFirst()
             .map(resource -> new UniversalAsset(resource, resourceAccess))
                    .map(asset -> {
                        log.trace("Found Asset for {}. UUID: '{}': {}", referencable, referencable.jcrUUID(), asset);
                        return asset;
                    });
            assetNullable.ifPresentOrElse(
                    asset -> log.debug(
                            "For {} (UUID: '{}') this Asset was found: {}", referencable, referencable.jcrUUID(), asset
                    ),
                    () -> log.debug("No Asset found for {}. UUID: '{}'", referencable, referencable.jcrUUID())
            );
            return assetNullable;
        }
    }
}