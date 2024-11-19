package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.jcr.Repository;
import javax.jcr.query.Query;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

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

    private final FullResourceAccess fullResourceAccess;

    /**
     * Constructs an instance of this class.
     * @param fullResourceAccess {@link FullResourceAccess} that will be used to acquire access to resources
     */
    @Activate
    public AssetsRepository(
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            FullResourceAccess fullResourceAccess
    ) {
        this.fullResourceAccess = fullResourceAccess;
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
        String query = buildQuery(referencable);
        log.trace("Searching for Asset for {}. UUID: '{}'. Query: {}", referencable, referencable.jcrUUID(), query);
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            Optional<Asset> assetNullable = new UnwrappedIteration<>(
                    resourceResolver.findResources(query, Query.JCR_SQL2)
            ).stream()
            .findFirst()
            .filter(resource -> new NodeProperties(
                    new TargetJCRPath(resource), fullResourceAccess).isPrimaryType(Asset.SUPPORTED_PRIMARY_TYPES)
            )
            .map(resource -> new UniversalAsset(resource, fullResourceAccess))
            .map(
                    asset -> {
                        log.trace("Found Asset for {}. UUID: '{}': {}", referencable, referencable.jcrUUID(), asset);
                        return asset;
                    }
            );
            assetNullable.ifPresentOrElse(
                    asset -> log.debug(
                            "For {} (UUID: '{}') this Asset was found: {}", referencable, referencable.jcrUUID(), asset
                    ),
                    () -> log.debug("No Asset found for {}. UUID: '{}'", referencable, referencable.jcrUUID())
            );
            return assetNullable;
        }
    }

    /**
     * Finds all {@link Asset}s that are located at the specified {@link JCRPath}. All and exclusively {@link Asset}s
     * that are located exactly at the specified {@link JCRPath} and its descendants are returned.
     * @param searchedPath {@link JCRPath} where the {@link Asset}s are searched
     * @return all {@link Asset}s that are located at the specified {@link JCRPath}
     */
    @SuppressWarnings("WeakerAccess")
    public List<Asset> find(JCRPath searchedPath) {
        log.debug("Searching for Assets at {}", searchedPath);
        StringJoiner nodeTypesQueryPart = new StringJoiner(" OR ");
        Asset.SUPPORTED_PRIMARY_TYPES.forEach(
                primaryType -> nodeTypesQueryPart.add(
                        String.format("node.[%s] = '%s'", JcrConstants.JCR_PRIMARYTYPE, primaryType)
                )
        );
        String query = String.format(
                "SELECT * FROM [%s] AS node WHERE (%s) AND ISDESCENDANTNODE(node, '%s')",
                JcrConstants.NT_BASE, nodeTypesQueryPart, searchedPath.get()
        );
        log.trace("This query was built to retrieve Assets: {}", query);
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            List<Asset> allAssets = new UnwrappedIteration<>(
                    resourceResolver.findResources(query, Query.JCR_SQL2)
            ).stream()
                    .filter(resource -> new NodeProperties(
                            new TargetJCRPath(resource), fullResourceAccess).isPrimaryType(
                                    Asset.SUPPORTED_PRIMARY_TYPES
                            )
                    )
                    .<Asset>map(jcrPath -> new UniversalAsset(new TargetJCRPath(jcrPath), fullResourceAccess))
                    .distinct()
                    .toList();
            log.debug("Found {} Assets with this query: {}", allAssets.size(), query);
            return allAssets;
        }
    }

    /**
     * Calculates the {@link DataSize} of binaries for all {@link Asset}s stored in the {@link Repository} and located
     * at the specified {@link JCRPath}. All and exclusively {@link Asset}s that are located exactly at the specified
     * {@link JCRPath} and its descendants are considered.
     * @param searchedPath {@link JCRPath} where the {@link Asset}s are searched
     * @return size of binaries for all {@link Asset}s stored in the {@link Repository} and located
     *         at the specified {@link JCRPath}
     */
    public DataSize size(JCRPath searchedPath) {
        log.debug("Calculating size of Assets at {}", searchedPath);
        DataSize dataSize = find(searchedPath).stream()
                .map(Asset::assetFile)
                .map(AssetFile::size)
                .reduce(DataSize::add)
                .orElse(new DataSize(NumberUtils.LONG_ZERO, DataUnit.BYTES));
        log.debug("Size of Assets at {} is {}", searchedPath, dataSize);
        return dataSize;
    }

    /**
     * Calculates the {@link DataSize} of binaries for all {@link Asset}s stored in the {@link Repository}.
     * @return size of binaries for all {@link Asset}s stored in the {@link Repository}
     */
    public DataSize size() {
        log.debug("Calculating size of all Assets");
        return size(new TargetJCRPath("/"));
    }

    /**
     * Retrieves all {@link Asset}s stored in the {@link Repository}.
     * @return all {@link Asset}s stored in the {@link Repository}
     */
    public List<Asset> all() {
        log.debug("Retrieving all Assets");
        return find(new TargetJCRPath("/"));
    }

    private String buildQuery(Referencable referencable) {
        StringJoiner nodeTypesQueryPart = new StringJoiner(" OR ");
        Asset.SUPPORTED_PRIMARY_TYPES.forEach(
                primaryType -> nodeTypesQueryPart.add(
                        String.format("node.[%s] = '%s'", JcrConstants.JCR_PRIMARYTYPE, primaryType)
                )
        );
        String query = String.format(
                "SELECT * FROM [%s] AS node "
              + "WHERE node.[%s] = '%s' "
              + "AND (%s)",
                JcrConstants.NT_BASE,
                JcrConstants.JCR_UUID, referencable.jcrUUID(),
                nodeTypesQueryPart
        );
        log.trace("For {} this query was built: {}", referencable, query);
        return query;
    }
}
