package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.network.Request;
import eu.ciechanowiec.sling.rocket.network.RequestWithDecomposition;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ToString
class RequestDelete implements RequestWithDecomposition {

    private final Request request;
    @ToString.Exclude
    private final MemoizingSupplier<Optional<Asset>> matchingAsset;

    RequestDelete(Request request) {
        this.request = request;
        matchingAsset = new MemoizingSupplier<>(
                () -> request.secondSelector()
                        .flatMap(
                                jcrUUID -> new AssetsRepository(
                                        request.userResourceAccess()
                                ).find((Referencable) () -> jcrUUID)
                        )
                        .filter(
                                asset -> {
                                    AssetDescriptor actualAssetDescriptor = new AssetDescriptor(this);
                                    AssetDescriptor expectedAssetDescriptor = new AssetDescriptor(asset);
                                    boolean areMatchingDescriptors = expectedAssetDescriptor.equals(
                                            actualAssetDescriptor
                                    );
                                    log.trace(
                                            "For {} and {} expected asset descriptor is '{}'. "
                                                    + "Actual asset descriptor is '{}'. "
                                                    + "Are matching: {}",
                                            asset, request, expectedAssetDescriptor,
                                            actualAssetDescriptor, areMatchingDescriptors
                                    );
                                    return areMatchingDescriptors;
                                }
                        )
                        .map(asset -> {
                            log.trace("For {} this asset was matched: {}", request, asset);
                            return asset;
                        })
        );
    }

    Optional<Asset> targetAsset() {
        return matchingAsset.get();
    }

    @Override
    public String contentPath() {
        return request.contentPath();
    }

    @Override
    public Optional<String> firstSelector() {
        return request.firstSelector();
    }

    @Override
    public Optional<String> secondSelector() {
        return request.secondSelector();
    }

    @Override
    public Optional<String> thirdSelector() {
        return request.thirdSelector();
    }

    @Override
    public Optional<String> selectorString() {
        return request.selectorString();
    }

    @Override
    public int numOfSelectors() {
        return request.numOfSelectors();
    }

    @Override
    public Optional<String> extension() {
        return request.extension();
    }

    boolean isValidStructure() {
        return new RequestStructure(this).isValid();
    }
}
