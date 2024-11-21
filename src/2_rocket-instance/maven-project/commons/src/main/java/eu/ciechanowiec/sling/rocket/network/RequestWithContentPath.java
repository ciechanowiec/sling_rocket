package eu.ciechanowiec.sling.rocket.network;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

/**
 * Wrapper around {@link SlingHttpServletRequest} that provides additional
 * functionality to the wrapped object related to request content path.
 */
@FunctionalInterface
public interface RequestWithContentPath {

    /**
     * Returns the value returned by {@link RequestPathInfo#getResourcePath()}
     * for the wrapped {@link SlingHttpServletRequest}.
     * @return value returned by {@link RequestPathInfo#getResourcePath()}
     *         for the wrapped {@link SlingHttpServletRequest}
     */
    String contentPath();
}
