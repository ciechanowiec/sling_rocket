package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.PersistenceException;

import java.util.Optional;

@Slf4j
class SafeSaving {

    private final StagedAssetReal stagedAssetReal;

    SafeSaving(StagedAssetReal stagedAssetReal) {
        this.stagedAssetReal = stagedAssetReal;
    }

    @SneakyThrows
    @SuppressWarnings(
        {
            "IllegalCatch", "ReturnCount", "MethodWithMultipleReturnPoints", "PMD.AvoidCatchingGenericException"
        }
    )
    Optional<Asset> save(TargetJCRPath targetJCRPath) {
        try {
            return Optional.of(stagedAssetReal.save(targetJCRPath));
        } catch (IllegalArgumentException exception) {
            boolean isExpectedMessage = exception.getMessage().equals("Can't create child on a synthetic root");
            Conditional.isTrueOrThrow(isExpectedMessage, exception);
            String message = "Failed to save %s to %s".formatted(stagedAssetReal, targetJCRPath);
            log.warn(message, exception);
            return Optional.empty();
        } catch (
            @SuppressWarnings({"OverlyBroadCatchBlock", "squid:S2221"})
            Exception exception) {
            boolean isPersistenceException = exception.getClass().isAssignableFrom(PersistenceException.class);
            Conditional.isTrueOrThrow(isPersistenceException, exception);
            String message = "Failed to save %s to %s".formatted(stagedAssetReal, targetJCRPath);
            log.warn(message, exception);
            return Optional.empty();
        }
    }
}
