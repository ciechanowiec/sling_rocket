package eu.ciechanowiec.sling.rocket.network;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

import java.util.Optional;

/**
 * Wrapper around {@link SlingJakartaHttpServletRequest} that provides additional functionality to the wrapped object
 * related to the request extension.
 */
@FunctionalInterface
public interface SlingRequestWithExtension {

    /**
     * Returns an {@link Optional} containing the extension returned by {@link RequestPathInfo#getExtension()} for the
     * wrapped {@link SlingJakartaHttpServletRequest}. If there is no such extension, an empty {@link Optional} is
     * returned.
     *
     * @return {@link Optional} containing the extension returned by {@link RequestPathInfo#getExtension()} for the
     * wrapped {@link SlingJakartaHttpServletRequest}; if there is no such extension, an empty {@link Optional} is
     * returned
     */
    Optional<String> extension();
}
