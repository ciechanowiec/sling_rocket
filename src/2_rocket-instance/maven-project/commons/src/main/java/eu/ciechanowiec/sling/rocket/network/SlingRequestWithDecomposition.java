package eu.ciechanowiec.sling.rocket.network;

/**
 * Union of {@link SlingRequestWithContentPath}, {@link SlingRequestWithSelectors} and
 * {@link SlingRequestWithExtension}.
 */
@SuppressWarnings("WeakerAccess")
public interface SlingRequestWithDecomposition
    extends SlingRequestWithContentPath, SlingRequestWithSelectors, SlingRequestWithExtension {

}
