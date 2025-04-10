package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;

class AssetsStats {

    private final AssetsRepository assetsRepository;
    private final MemoizingSupplier<Long> numberOfAllAssets;
    private final MemoizingSupplier<DataSize> dataSizeOfAllAssets;

    @SuppressWarnings("PMD.UnnecessaryCast")
    AssetsStats(FullResourceAccess fullResourceAccess) {
        this.assetsRepository = new AssetsRepository(fullResourceAccess);
        this.numberOfAllAssets = new MemoizingSupplier<>(() -> (long) assetsRepository.all().size());
        this.dataSizeOfAllAssets = new MemoizingSupplier<>(assetsRepository::size);
    }

    @JsonProperty("numberOfAllAssets")
    long numberOfAllAssets() {
        return numberOfAllAssets.get();
    }

    @JsonProperty("biggestAssets")
    List<String> biggestAssets() {
        return assetsRepository.all()
            .stream()
            .sorted((assetOne, assetTwo) -> assetTwo.assetFile().size().compareTo(assetOne.assetFile().size()))
            .limit(100)
            .map(asset -> String.format("'%s' - %s", asset.jcrPath().get(), asset.assetFile().size()))
            .toList();
    }

    @JsonProperty("dataSizeOfAllAssets")
    String dataSizeOfAllAssets() {
        return dataSizeOfAllAssets.get().toString();
    }

    @JsonProperty("averageAssetSize")
    String averageAssetSize() {
        return Optional.ofNullable(
                Conditional.conditional(numberOfAllAssets() > 0)
                    .onFalse(() -> new DataSize(NumberUtils.LONG_ZERO, DataUnit.BYTES))
                    .onTrue(() -> new DataSize(
                        dataSizeOfAllAssets.get().bytes() / numberOfAllAssets(), DataUnit.BYTES)
                    )
                    .get(DataSize.class)
            )
            .orElseThrow()
            .toString();
    }
}
