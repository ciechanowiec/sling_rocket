package eu.ciechanowiec.sling.rocket.jcr.path;

/**
 * Represents an abstraction that has an associated {@link JCRPath}.
 */
@FunctionalInterface
public interface WithJCRPath {

    /**
     * Returns the {@link JCRPath} associated with this abstraction.
     * @return {@link JCRPath} associated with this abstraction
     */
    JCRPath jcrPath();
}
