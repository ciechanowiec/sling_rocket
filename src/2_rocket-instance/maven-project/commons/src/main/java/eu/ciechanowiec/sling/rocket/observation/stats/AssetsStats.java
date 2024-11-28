package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class AssetsStats {

    private final AssetsRepository assetsRepository;
    private final CompletableFuture<Long> numberOfAllAssets;
    private final CompletableFuture<DataSize> dataSizeOfAllAssets;

    @SuppressWarnings("PMD.UnnecessaryCast")
    AssetsStats(FullResourceAccess fullResourceAccess) {
        this.assetsRepository = new AssetsRepository(fullResourceAccess);
        this.numberOfAllAssets = CompletableFuture.supplyAsync(() -> (long) assetsRepository.all().size());
        this.dataSizeOfAllAssets = CompletableFuture.supplyAsync(assetsRepository::size);
    }

    @JsonProperty("numberOfAllAssets")
    long numberOfAllAssets() {
        return numberOfAllAssets.join();
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
        return dataSizeOfAllAssets.join().toString();
    }

    @JsonProperty("averageAssetSize")
    String averageAssetSize() {
        return Optional.ofNullable(
                        Conditional.conditional(numberOfAllAssets() > 0)
                                .onFalse(() -> new DataSize(NumberUtils.LONG_ZERO, DataUnit.BYTES))
                                .onTrue(() -> new DataSize(
                                        dataSizeOfAllAssets.join().bytes() / numberOfAllAssets(), DataUnit.BYTES)
                                )
                                .get(DataSize.class)
                )
                .orElseThrow()
                .toString();
    }
}
