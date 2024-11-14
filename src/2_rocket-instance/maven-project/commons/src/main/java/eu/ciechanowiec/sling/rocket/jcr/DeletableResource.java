package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Repository;
import java.util.Optional;

/**
 * Represents a {@link Resource} that can be deleted from the {@link Repository}.
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
public class DeletableResource {

    private final JCRPath jcrPath;
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     * @param jcrPath {@link JCRPath} to the {@link Resource} to be deleted from the {@link Repository}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                       object to acquire access to resources
     */
    @SuppressWarnings("WeakerAccess")
    public DeletableResource(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
    }

    /**
     * Constructs an instance of this class.
     * @param withJCRPath object that contains a {@link JCRPath} to the {@link Resource} to be deleted
     *        from the {@link Repository}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                       object to acquire access to resources
     */
    public DeletableResource(WithJCRPath withJCRPath, ResourceAccess resourceAccess) {
        this(withJCRPath.jcrPath(), resourceAccess);
    }

    /**
     * Deletes the wrapped {@link Resource} from the {@link Repository}.
     * @return {@link Optional} containing the {@link JCRPath} of the deleted {@link Resource};
     *         empty {@link Optional} is returned if the {@link Resource} was not found
     */
    public Optional<JCRPath> delete() {
        log.trace("Deleting {}", jcrPath);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.getResource(jcrPath.get()))
                    .map(
                            SneakyFunction.sneaky(
                                    resource -> {
                                        log.trace("Deleting {}", resource);
                                        resourceResolver.delete(resource);
                                        resourceResolver.commit();
                                        log.trace("Deleted {}", jcrPath);
                                        return jcrPath;
                                    }
                            )
                    ).or(() -> {
                        log.trace("Resource at {} not found and won't be deleted", jcrPath);
                        return Optional.empty();
                    });
        }
    }
}
