package eu.ciechanowiec.sling.rocket.network;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

import java.util.Optional;

/**
 * Wrapper around {@link SlingJakartaHttpServletRequest} that provides additional functionality to the wrapped object
 * related to request suffix.
 */
@FunctionalInterface
public interface SlingRequestWithSuffix {

    /**
     * Returns an {@link Optional} containing the suffix returned by {@link RequestPathInfo#getSuffix()} for the wrapped
     * {@link SlingJakartaHttpServletRequest}. If there is no such suffix, an empty {@link Optional} is returned.
     *
     * @return {@link Optional} containing the suffix returned by {@link RequestPathInfo#getSuffix()} for the wrapped
     * {@link SlingJakartaHttpServletRequest}; if there is no such suffix, an empty {@link Optional} is returned
     */
    Optional<String> suffix();
}
