package eu.ciechanowiec.sling.rocket.observation.stats;

import eu.ciechanowiec.sling.rocket.commons.JSON;

/**
 * Unit of statistics of a Sling Rocket application.
 * <p>
 * Every {@link RocketStats} instance must be serializable into JSON by Jackson.
 */
public interface RocketStats extends JSON, Comparable<RocketStats> {

    /**
     * Returns the unique name of this {@link RocketStats}. Among others, it can be a fully qualified name of the class
     * implementing {@link RocketStats}.
     *
     * @return unique name of this {@link RocketStats}
     */
    String name();

    @Override
    default int compareTo(RocketStats other) {
        return this.name().compareTo(other.name());
    }
}
