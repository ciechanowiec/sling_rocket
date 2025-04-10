package eu.ciechanowiec.sling.rocket.network;

import java.util.Optional;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

/**
 * Wrapper around {@link SlingHttpServletRequest} that provides additional functionality to the wrapped object related
 * to request extension.
 */
@FunctionalInterface
public interface RequestWithExtension {

    /**
     * Returns an {@link Optional} containing the extension returned by {@link RequestPathInfo#getExtension()} for the
     * wrapped {@link SlingHttpServletRequest}. If there is no such extension, an empty {@link Optional} is returned.
     *
     * @return {@link Optional} containing the extension returned by {@link RequestPathInfo#getExtension()} for the
     * wrapped {@link SlingHttpServletRequest}; if there is no such extension, an empty {@link Optional} is returned
     */
    Optional<String> extension();
}
