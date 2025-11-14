package eu.ciechanowiec.sling.rocket.observation.stats;

import eu.ciechanowiec.sling.rocket.commons.JSON;

/**
 * Unit of statistics of a Sling Rocket application.
 * <p>
 * Every {@link RocketStats} instance must be serializable into JSON by Jackson.
 */
public interface RocketStats extends JSON {

    /**
     * Returns the unique name of this {@link RocketStats}. Among others, it can be a fully qualified name of the class
     * implementing {@link RocketStats}.
     *
     * @return unique name of this {@link RocketStats}
     */
    String name();
}
