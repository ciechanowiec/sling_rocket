package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.function.Function;

/**
 * Represents a {@link HttpHeaders#CONTENT_DISPOSITION} HTTP header
 */
public enum ContentDispositionHeader {

    /**
     * {@link HttpHeaders#CONTENT_DISPOSITION} HTTP header of type {@code attachment}.
     */
    ATTACHMENT(
        asset -> {
            String assetReadableName = new AssetReadableName(asset).get();
            return "attachment;filename=\"%s\"".formatted(assetReadableName);
        }
    ),

    /**
     * {@link HttpHeaders#CONTENT_DISPOSITION} HTTP header of type {@code inline}.
     */
    INLINE(asset -> "inline");

    private final Function<Asset, String> value;

    ContentDispositionHeader(Function<Asset, String> value) {
        this.value = value;
    }

    /**
     * Returns the value of this {@link ContentDispositionHeader} for the specified {@link Asset}.
     *
     * @param asset {@link Asset} for which the value of this {@link ContentDispositionHeader} will be returned
     * @return value of this {@link ContentDispositionHeader} for the specified {@link Asset}
     */
    public String value(Asset asset) {
        return value.apply(asset);
    }
}
