package eu.ciechanowiec.sling.rocket.asset.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetFile;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.network.Affected;
import eu.ciechanowiec.sling.rocket.network.RequestWithDecomposition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
class AssetDescriptor implements Affected {

    private final MemoizingSupplier<String> source;
    private final MemoizingSupplier<String> originalFileName;
    private final MemoizingSupplier<String> downloadLink;

    AssetDescriptor(Asset asset) {
        this(asset, StringUtils.EMPTY);
    }

    AssetDescriptor(Asset asset, String downloadLink) {
        this.source = new MemoizingSupplier<>(() -> {
            String assetDescriptorFromAsset = String.format(
                "%s%s", asset.jcrUUID(), asset.assetMetadata().filenameExtension().orElse(".file")
            );
            log.trace("For {} this descriptor was generated: '{}'", asset, assetDescriptorFromAsset);
            return assetDescriptorFromAsset;
        });
        this.originalFileName = new MemoizingSupplier<>(
            () -> asset.assetMetadata()
                .properties()
                .map(nodeProperties -> nodeProperties.propertyValue(
                    AssetFile.PN_ORIGINAL_NAME, DefaultProperties.STRING_EMPTY)
                ).orElse(StringUtils.EMPTY)
        );
        this.downloadLink = new MemoizingSupplier<>(() -> downloadLink);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    AssetDescriptor(RequestDelete requestDelete) {
        this(requestDelete, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    AssetDescriptor(RequestDownload requestDownload) {
        this(requestDownload, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    private AssetDescriptor(RequestWithDecomposition request, String originalFileName, String downloadLink) {
        this.source = new MemoizingSupplier<>(() -> {
            String assetDescriptorFromRequest = String.format(
                "%s.%s",
                request.secondSelector().orElse(StringUtils.EMPTY),
                request.extension().orElse(StringUtils.EMPTY)
            );
            log.trace("For {} this descriptor was generated: '{}'", request, assetDescriptorFromRequest);
            return assetDescriptorFromRequest;
        });
        this.originalFileName = new MemoizingSupplier<>(() -> originalFileName);
        this.downloadLink = new MemoizingSupplier<>(() -> downloadLink);
    }

    @Override
    @JsonProperty("assetDescriptor")
    public String toString() {
        return source.get();
    }

    @JsonProperty(AssetFile.PN_ORIGINAL_NAME)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String originalFileName() {
        return originalFileName.get();
    }

    @JsonProperty("assetDownloadLink")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String downloadLink() {
        return downloadLink.get();
    }

    @Override
    @SuppressWarnings(
        {
            "SimplifiableIfStatement", "AccessingNonPublicFieldOfAnotherObject", "PMD.SimplifyBooleanReturns"
        }
    )
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        return comparedObject instanceof AssetDescriptor comparedAssetDescriptor
            && source.get().equals(comparedAssetDescriptor.source.get());
    }

    @Override
    public int hashCode() {
        return source.get().hashCode() * 31;
    }
}
