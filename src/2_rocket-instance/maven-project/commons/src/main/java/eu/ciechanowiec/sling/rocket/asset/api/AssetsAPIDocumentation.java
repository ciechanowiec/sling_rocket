package eu.ciechanowiec.sling.rocket.asset.api;

/**
 * Documentation for Assets API.
 */
@FunctionalInterface
public interface AssetsAPIDocumentation {

    /**
     * Returns generic HTML page that describes Assets API.
     *
     * @return HTML page that describes Assets API
     */
    String html();
}
