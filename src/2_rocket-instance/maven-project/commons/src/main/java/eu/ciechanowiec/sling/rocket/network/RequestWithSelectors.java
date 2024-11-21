package eu.ciechanowiec.sling.rocket.network;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

import java.util.Optional;

/**
 * Wrapper around {@link SlingHttpServletRequest} that provides additional
 * functionality to the wrapped object related to request selectors.
 */
public interface RequestWithSelectors {

    /**
     * Returns an {@link Optional} containing the <b>first</b> selector among selectors returned by
     * {@link RequestPathInfo#getSelectors()} for the wrapped {@link SlingHttpServletRequest}.
     * If there are no selectors, an empty {@link Optional} is returned.
     * @return {@link Optional} containing the <b>first</b> selector among selectors returned by
     *         {@link RequestPathInfo#getSelectors()} for the wrapped {@link SlingHttpServletRequest};
     *         if there are no selectors, an empty {@link Optional} is returned
     */
    Optional<String> firstSelector();

    /**
     * Returns an {@link Optional} containing the <b>second</b> selector among selectors returned by
     * {@link RequestPathInfo#getSelectors()} for the wrapped {@link SlingHttpServletRequest}.
     * If there is no such selector, an empty {@link Optional} is returned.
     * @return {@link Optional} containing the <b>second</b> selector among selectors returned by
     *         {@link RequestPathInfo#getSelectors()} for the wrapped {@link SlingHttpServletRequest};
     *         if there is no such selector, an empty {@link Optional} is returned
     */
    Optional<String> secondSelector();

    /**
     * Returns an {@link Optional} containing the <b>third</b> selector among selectors returned by
     * {@link RequestPathInfo#getSelectors()} for the wrapped {@link SlingHttpServletRequest}.
     * If there is no such selector, an empty {@link Optional} is returned.
     * @return {@link Optional} containing the <b>third</b> selector among selectors returned by
     *         {@link RequestPathInfo#getSelectors()} for the wrapped {@link SlingHttpServletRequest};
     *         if there is no such selector, an empty {@link Optional} is returned
     */
    Optional<String> thirdSelector();

    /**
     * Returns an {@link Optional} containing a selector {@link String} returned by
     * {@link RequestPathInfo#getSelectorString()} for the wrapped {@link SlingHttpServletRequest}.
     * If there is no such selector {@link String}, an empty {@link Optional} is returned.
     * @return {@link Optional} containing a selector {@link String} returned by
     *         {@link RequestPathInfo#getSelectorString()} for the wrapped {@link SlingHttpServletRequest};
     *         if there is no such selector {@link String}, an empty {@link Optional} is returned
     */
    Optional<String> selectorString();

    /**
     * Returns the number of selectors returned by {@link RequestPathInfo#getSelectors()}
     * for the wrapped {@link SlingHttpServletRequest}. If there are no selectors, {@code 0} is returned.
     * @return number of selectors returned by {@link RequestPathInfo#getSelectors()} for the wrapped
     *         {@link SlingHttpServletRequest}. If there are no selectors, {@code 0} is returned.
     */
    int numOfSelectors();
}
