package eu.ciechanowiec.sling.rocket.unit;

/**
 * Digital information that has known {@link DataSize}.
 */
@FunctionalInterface
public interface WithDataSize {

    /**
     * Returns the {@link DataSize} of the related digital information.
     *
     * @return {@link DataSize} of the related digital information
     */
    DataSize dataSize();
}
