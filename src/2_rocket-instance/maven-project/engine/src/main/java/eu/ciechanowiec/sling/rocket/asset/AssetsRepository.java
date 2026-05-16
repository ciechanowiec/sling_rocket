package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.ref.Referenceable;
import eu.ciechanowiec.sling.rocket.jcr.ref.ReferenceableResolvable;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Repository;
import javax.jcr.query.Query;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
        log.trace(
            "{} searching for Asset for {}. UUID: '{}'", this, referenceable, referenceable.jcrUUID()
        );
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Optional<Asset> assetNullable = new ReferenceableResolvable(referenceable, resourceResolver)
                .resource()
                .filter(resource -> new NodeProperties(resource).isPrimaryType(Asset.SUPPORTED_PRIMARY_TYPES))
                .map(resource -> new UniversalAsset(resource, resourceAccess));
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
        String query = buildPathQuery(searchedPath);
        log.trace("This query was built by {} to retrieve Assets: {}", this, query);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            List<Asset> allAssets = lazyStream(resourceResolver.findResources(query, Query.JCR_SQL2))
                .filter(resource -> new NodeProperties(resource).isPrimaryType(Asset.SUPPORTED_PRIMARY_TYPES))
                .<Asset>map(resource -> new UniversalAsset(new TargetJCRPath(resource), resourceAccess))
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
        String query = buildPathQuery(searchedPath);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            long totalBytes = lazyStream(resourceResolver.findResources(query, Query.JCR_SQL2))
                .filter(resource -> new NodeProperties(resource).isPrimaryType(Asset.SUPPORTED_PRIMARY_TYPES))
                .map(resource -> new UniversalAsset(new TargetJCRPath(resource), resourceAccess))
                .distinct()
                .map(Asset::assetFile)
                .map(AssetFile::size)
                .mapToLong(DataSize::bytes)
                .sum();
            DataSize dataSize = new DataSize(totalBytes, DataUnit.BYTES);
            log.debug("Size of Assets at {} is {}. Calculated by {}", searchedPath, dataSize, this);
            return dataSize;
        }
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

    private String buildPathQuery(JCRPath searchedPath) {
        StringJoiner nodeTypesQueryPart = new StringJoiner(" OR ");
        Asset.SUPPORTED_PRIMARY_TYPES.forEach(
            primaryType -> nodeTypesQueryPart.add(
                String.format("node.[%s] = '%s'", JcrConstants.JCR_PRIMARYTYPE, primaryType)
            )
        );
        return String.format(
            "SELECT * FROM [%s] AS node WHERE (%s) AND ISDESCENDANTNODE(node, '%s')",
            JcrConstants.NT_BASE, nodeTypesQueryPart, searchedPath.get()
        );
    }

    private Stream<Resource> lazyStream(Iterator<Resource> iterator) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false
        );
    }
}
