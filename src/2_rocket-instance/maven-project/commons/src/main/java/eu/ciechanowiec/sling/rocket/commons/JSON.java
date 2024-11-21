package eu.ciechanowiec.sling.rocket.commons;

/**
 * Object that can be represented as JSON.
 */
@FunctionalInterface
public interface JSON {

    /**
     * Returns the JSON representation of this object.
     * @return JSON representation of this object
     */
    String asJSON();
}
