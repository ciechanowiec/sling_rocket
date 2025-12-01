package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.ref.Referenceable;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Repository;
import javax.jcr.query.Query;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Repository for {@link Asset}s.
 */
@Slf4j
@ToString
public class AssetsRepository {

    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param resourceAccess {@link ResourceAccess} that will be used to acquire access to resources
     */
    public AssetsRepository(ResourceAccess resourceAccess) {
        this.resourceAccess = resourceAccess;
        log.trace("Initialized {}", this);
    }

    /**
     * Finds an {@link Asset} for the given {@link Referenceable}.
     *
     * @param referenceable {@link Referenceable} pointing to the searched {@link Asset}
     * @return {@link Optional} containing the found {@link Asset}; empty {@link Optional} is returned if no related
     * {@link Asset} was found
     */
    @SuppressWarnings("WeakerAccess")
    public Optional<Asset> find(Referenceable referenceable) {
        String query = buildQuery(referenceable);
        log.trace(
            "{} searching for Asset for {}. UUID: '{}'. Query: {}",
            this, referenceable, referenceable.jcrUUID(), query
        );
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Optional<Asset> assetNullable = IteratorUtils.toList(resourceResolver.findResources(query, Query.JCR_SQL2))
                .stream()
                .findFirst()
                .filter(resource -> new NodeProperties(
                    new TargetJCRPath(resource), resourceAccess).isPrimaryType(Asset.SUPPORTED_PRIMARY_TYPES)
                )
                .map(resource -> new UniversalAsset(resource, resourceAccess))
                .map(
                    asset -> {
                        log.trace(
                            "{} found Asset for {}. UUID: '{}': {}",
                            this, referenceable, referenceable.jcrUUID(), asset
                        );
                        return asset;
                    }
                );
            assetNullable.ifPresentOrElse(
                asset -> log.debug(
                    "For {} (UUID: '{}') this Asset was found: {} by {}",
                    referenceable, referenceable.jcrUUID(), asset, this
                ),
                () -> log.debug(
                    "No Asset found for {} by {}. UUID: '{}'", referenceable, this, referenceable.jcrUUID()
                )
            );
            return assetNullable;
        }
    }

    /**
     * Finds all {@link Asset}s that are located at the specified {@link JCRPath}. All and exclusively {@link Asset}s
     * that are located exactly at the specified {@link JCRPath} and its descendants are returned.
     *
     * @param searchedPath {@link JCRPath} where the {@link Asset}s are searched
     * @return all {@link Asset}s that are located at the specified {@link JCRPath}
     */
    @SuppressWarnings("WeakerAccess")
    public List<Asset> find(JCRPath searchedPath) {
        log.debug("{} searching for Assets at {}", this, searchedPath);
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
        log.trace("This query was built by {} to retrieve Assets: {}", this, query);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            List<Asset> allAssets = IteratorUtils.toList(resourceResolver.findResources(query, Query.JCR_SQL2)).stream()
                .filter(resource -> new NodeProperties(
                        new TargetJCRPath(resource), resourceAccess).isPrimaryType(
                        Asset.SUPPORTED_PRIMARY_TYPES
                    )
                )
                .<Asset>map(jcrPath -> new UniversalAsset(new TargetJCRPath(jcrPath), resourceAccess))
                .distinct()
                .toList();
            log.debug("{} found {} Assets with this query: {}", this, allAssets.size(), query);
            return allAssets;
        }
    }

    /**
     * Calculates the {@link DataSize} of binaries for all {@link Asset}s stored in the {@link Repository} and located
     * at the specified {@link JCRPath}. All and exclusively {@link Asset}s that are located exactly at the specified
     * {@link JCRPath} and its descendants are considered.
     *
     * @param searchedPath {@link JCRPath} where the {@link Asset}s are searched
     * @return size of binaries for all {@link Asset}s stored in the {@link Repository} and located at the specified
     * {@link JCRPath}
     */
    public DataSize size(JCRPath searchedPath) {
        log.debug("{} calculating size of Assets at {}", this, searchedPath);
        DataSize dataSize = find(searchedPath).stream()
            .map(Asset::assetFile)
            .map(AssetFile::size)
            .reduce(DataSize::add)
            .orElse(new DataSize(NumberUtils.LONG_ZERO, DataUnit.BYTES));
        log.debug("Size of Assets at {} is {}. Calculated by {}", searchedPath, dataSize, this);
        return dataSize;
    }

    /**
     * Calculates the {@link DataSize} of binaries for all {@link Asset}s stored in the {@link Repository}.
     *
     * @return size of binaries for all {@link Asset}s stored in the {@link Repository}
     */
    public DataSize size() {
        log.debug("{} calculating size of all Assets", this);
        return size(new TargetJCRPath("/"));
    }

    /**
     * Retrieves all {@link Asset}s stored in the {@link Repository}.
     *
     * @return all {@link Asset}s stored in the {@link Repository}
     */
    public List<Asset> all() {
        log.debug("{} retrieving all Assets", this);
        return find(new TargetJCRPath("/"));
    }

    private String buildQuery(Referenceable referenceable) {
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
            JcrConstants.JCR_UUID, referenceable.jcrUUID(),
            nodeTypesQueryPart
        );
        log.trace("For {} this query was built by {}: {}", referenceable, this, query);
        return query;
    }
}
