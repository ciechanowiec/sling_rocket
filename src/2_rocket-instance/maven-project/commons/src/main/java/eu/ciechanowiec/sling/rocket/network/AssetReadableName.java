package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetMetadata;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

@Slf4j
class AssetReadableName {

    private final Asset asset;

    AssetReadableName(Asset asset) {
        this.asset = asset;
    }

    String get() {
        MimeTypes defaultMimeTypes = MimeTypes.getDefaultMimeTypes();
        String resolvedExtension = Optional.of(asset)
            .map(Asset::assetMetadata)
            .map(AssetMetadata::mimeType)
            .flatMap(
                mimeTypeName -> {
                    try {
                        return Optional.of(defaultMimeTypes.forName(mimeTypeName));
                    } catch (MimeTypeException exception) {
                        String message = String.format(
                            "Unable to resolve the mime type '%s' for asset '%s'",
                            mimeTypeName, asset
                        );
                        log.error(message, exception);
                        return Optional.empty();
                    }
                }
            ).map(MimeType::getExtension)
            .filter(extension -> !extension.isBlank())
            .orElse(StringUtils.EMPTY);
        return String.format("%s%s", asset.jcrUUID(), resolvedExtension);
    }
}
