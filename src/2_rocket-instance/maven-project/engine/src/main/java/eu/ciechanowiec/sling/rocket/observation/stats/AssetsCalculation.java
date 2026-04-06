package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import lombok.SneakyThrows;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Optional;

class AssetsCalculation implements JSON {

    private final MemoizingSupplier<List<Asset>> allAssets;
    private final MemoizingSupplier<Long> numberOfAllAssets;
    private final MemoizingSupplier<DataSize> dataSizeOfAllAssets;

    AssetsCalculation(AssetsRepository assetsRepository) {
        this.allAssets = new MemoizingSupplier<>(assetsRepository::all);
        this.numberOfAllAssets = new MemoizingSupplier<>(() -> Long.valueOf(this.allAssets.get().size()));
        this.dataSizeOfAllAssets = new MemoizingSupplier<>(assetsRepository::size);
    }

    @JsonProperty
    long numberOfAllAssets() {
        return numberOfAllAssets.get();
    }

    @JsonProperty
    long dataSizeOfAllAssetsBytes() {
        return dataSizeOfAllAssets.get().bytes();
    }

    @JsonProperty
    String dataSizeOfAllAssetsReadable() {
        return dataSizeOfAllAssets.get().toString();
    }

    @JsonProperty
    long averageAssetSizeBytes() {
        return Optional.of(dataSizeOfAllAssets.get())
            .filter(_ -> numberOfAllAssets.get() > 0)
            .map(size -> size.bytes() / numberOfAllAssets.get())
            .orElse(NumberUtils.LONG_ZERO);
    }

    @JsonProperty
    String averageAssetSizeReadable() {
        return Optional.of(dataSizeOfAllAssets.get())
            .filter(_ -> numberOfAllAssets.get() > 0)
            .map(size -> size.bytes() / numberOfAllAssets.get())
            .map(averageInBytes -> new DataSize(averageInBytes, DataUnit.BYTES))
            .orElse(new DataSize(NumberUtils.LONG_ZERO, DataUnit.BYTES))
            .toString();
    }

    @JsonProperty
    List<String> biggestAssets() {
        return allAssets.get()
            .stream()
            .sorted((assetOne, assetTwo) -> assetTwo.assetFile().size().compareTo(assetOne.assetFile().size()))
            .limit(100)
            .map(asset -> String.format("'%s' - %s", asset.jcrPath().get(), asset.assetFile().size()))
            .toList();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
