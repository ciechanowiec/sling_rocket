package eu.ciechanowiec.sling.rocket.jcr.path;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Item;
import javax.jcr.Repository;
import java.util.Optional;

/**
 * Represents an absolute path to the persisted or hypothetically persisted {@link Item} in the {@link Repository}.
 */
@FunctionalInterface
public interface JCRPath {

    /**
     * Returns the path in the {@link Repository} represented by this object in a {@link String} format.
     * @return path in the {@link Repository} represented by this object in a {@link String} format
     * @throws InvalidJCRPathException if the path in the {@link Repository} represented by this object is not valid
     */
    String get();

    /**
     * Asserts that the path in the {@link Repository} represented by this object is
     * free and has no {@link Item} persisted.
     * @param resourceAccess {@link ResourceAccess} that will be used to acquire access to resources
     * @throws OccupiedJCRPathException if a path to an {@link Item} in the {@link Repository} represented by this
     *                                  object isn't free and has some {@link Item} persisted
     */
    default void assertThatJCRPathIsFree(ResourceAccess resourceAccess) {
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = get();
            Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                    .ifPresent(
                            resource -> {
                                String message = String.format(
                                        "This path is expected to be free: '%s'. But isn't: %s", jcrPathRaw, resource
                                );
                                throw new OccupiedJCRPathException(message);
                            }
                    );
        }
    }
}
