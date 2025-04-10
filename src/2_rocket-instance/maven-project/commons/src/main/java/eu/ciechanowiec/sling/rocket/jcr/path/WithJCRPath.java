package eu.ciechanowiec.sling.rocket.jcr.path;

/**
 * Entity that has an associated {@link JCRPath}.
 */
@FunctionalInterface
public interface WithJCRPath {

    /**
     * Returns the {@link JCRPath} associated with this entity.
     *
     * @return {@link JCRPath} associated with this entity
     */
    JCRPath jcrPath();
}
