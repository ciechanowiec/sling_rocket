package eu.ciechanowiec.sling.rocket.network;

/**
 * Union of {@link RequestWithContentPath}, {@link RequestWithSelectors} and {@link RequestWithExtension}.
 */
@SuppressWarnings("WeakerAccess")
public interface RequestWithDecomposition extends RequestWithContentPath, RequestWithSelectors, RequestWithExtension {

}
