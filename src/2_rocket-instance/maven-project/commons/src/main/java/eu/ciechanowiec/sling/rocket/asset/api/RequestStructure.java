package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.network.RequestWithDecomposition;
import eu.ciechanowiec.sling.rocket.network.RequestWithSelectors;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

@Slf4j
class RequestStructure {

    @SuppressWarnings("PMD.LinguisticNaming")
    private final Supplier<Boolean> isValid;

    private RequestStructure(
        String expectedStructureRegex, String actualStructure,
        int expectedNumOfSelectors, RequestWithSelectors request
    ) {
        isValid = () -> {
            log.trace(
                "Validating {}. Expected structure regex: '{}'. Actual structure: '{}'",
                request, expectedStructureRegex, actualStructure
            );
            boolean areMatchingStructures = actualStructure.matches(expectedStructureRegex);
            boolean isValidNumOfSelectors = request.numOfSelectors() == expectedNumOfSelectors;
            boolean noEmptySelectors = request.selectorString()
                .filter(selectorString -> selectorString.matches("(.*\\.\\..*|.*\\.$)"))
                .isEmpty();
            log.trace(
                "{}: are matching structures: {}, is valid number of selectors: {}, no empty selectors: {}",
                request, areMatchingStructures, isValidNumOfSelectors, noEmptySelectors
            );
            return areMatchingStructures && isValidNumOfSelectors && noEmptySelectors;
        };
    }

    RequestStructure(RequestDelete request) {
        isValid = () -> new RequestStructure(request, ServletDelete.SELECTOR).isValid();
    }

    RequestStructure(RequestDownload request) {
        isValid = () -> new RequestStructure(request, ServletDownload.SELECTOR).isValid();
    }

    private RequestStructure(RequestWithDecomposition request, String expectedFirstSelector) {
        isValid = () -> {
            String uuidRegex = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
            String expectedStructureRegex = String.format(
                "^%s\\.%s\\.%s\\.[0-9a-zA-Z]*$", AssetsAPI.ASSETS_API_PATH, expectedFirstSelector, uuidRegex
            );
            String actualStructure = String.format(
                "%s.%s.%s.%s",
                request.contentPath(),
                request.firstSelector().orElse(StringUtils.EMPTY),
                request.secondSelector().orElse(StringUtils.EMPTY),
                request.extension().orElse(StringUtils.EMPTY)
            );
            return new RequestStructure(expectedStructureRegex, actualStructure, 2, request).isValid();
        };
    }

    RequestStructure(RequestUpload request) {
        isValid = () -> {
            String expectedStructureRegex = String.format(
                "^%s\\.%s$", AssetsAPI.ASSETS_API_PATH, ServletUpload.EXTENSION
            );
            String actualStructure = String.format(
                "%s.%s",
                request.contentPath(),
                request.extension().orElse(StringUtils.EMPTY)
            );
            return new RequestStructure(
                expectedStructureRegex, actualStructure, NumberUtils.INTEGER_ZERO, request
            ).isValid();
        };
    }

    boolean isValid() {
        return isValid.get();
    }
}
