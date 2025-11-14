package eu.ciechanowiec.sling.rocket.network;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

/**
 * Wrapper around {@link SlingJakartaHttpServletRequest} that provides additional functionality to the wrapped object
 * related to the request content path.
 */
@FunctionalInterface
public interface SlingRequestWithContentPath {

    /**
     * Returns the value returned by {@link RequestPathInfo#getResourcePath()} for the wrapped
     * {@link SlingJakartaHttpServletRequest}.
     *
     * @return value returned by {@link RequestPathInfo#getResourcePath()} for the wrapped
     * {@link SlingJakartaHttpServletRequest}
     */
    String contentPath();
}
