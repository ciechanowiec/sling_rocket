package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.network.SlingRequest;
import eu.ciechanowiec.sling.rocket.network.SlingRequestWithDecomposition;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ToString
class RequestDownload implements SlingRequestWithDecomposition {

    private final SlingRequest slingRequest;
    @ToString.Exclude
    private final MemoizingSupplier<Optional<Asset>> matchingAsset;

    RequestDownload(SlingRequest slingRequest) {
        this.slingRequest = slingRequest;
        matchingAsset = new MemoizingSupplier<>(
            () -> slingRequest.secondSelector()
                .flatMap(
                    jcrUUID -> new AssetsRepository(
                        slingRequest.userResourceAccess()
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
                            asset, slingRequest, expectedAssetDescriptor,
                            actualAssetDescriptor, areMatchingDescriptors
                        );
                        return areMatchingDescriptors;
                    }
                )
                .map(asset -> {
                    log.trace("For {} this asset was matched: {}", slingRequest, asset);
                    return asset;
                })
        );
    }

    Optional<Asset> targetAsset() {
        return matchingAsset.get();
    }

    @Override
    public String contentPath() {
        return slingRequest.contentPath();
    }

    @Override
    public Optional<String> firstSelector() {
        return slingRequest.firstSelector();
    }

    @Override
    public Optional<String> secondSelector() {
        return slingRequest.secondSelector();
    }

    @Override
    public Optional<String> thirdSelector() {
        return slingRequest.thirdSelector();
    }

    @Override
    public Optional<String> selectorString() {
        return slingRequest.selectorString();
    }

    @Override
    public int numOfSelectors() {
        return slingRequest.numOfSelectors();
    }

    @Override
    public Optional<String> extension() {
        return slingRequest.extension();
    }

    boolean isValidStructure() {
        return new RequestStructure(this).isValid();
    }
}
