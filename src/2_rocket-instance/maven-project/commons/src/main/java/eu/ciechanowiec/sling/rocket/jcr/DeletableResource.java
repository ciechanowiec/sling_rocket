package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import eu.ciechanowiec.sling.rocket.privilege.RequiresPrivilege;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Represents a {@link Resource} that can be deleted from the {@link Repository}.
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
public class DeletableResource implements RequiresPrivilege {

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
     * @param withJCRPath object that contains a {@link JCRPath} to the {@link Resource}
     *                    to be deleted from the {@link Repository}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                       object to acquire access to resources
     */
    public DeletableResource(WithJCRPath withJCRPath, ResourceAccess resourceAccess) {
        this(withJCRPath.jcrPath(), resourceAccess);
    }

    /**
     * Deletes the wrapped {@link Resource} from the {@link Repository}.
     * @return {@link Optional} containing the {@link JCRPath} of the deleted {@link Resource};
     *         empty {@link Optional} is returned if the deletion operation didn't succeed
     *         due to {@link PersistenceException} or if the {@link Resource} was not found
     */
    public Optional<JCRPath> delete() {
        log.trace("Deleting {}", jcrPath);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.getResource(jcrPath.get()))
                    .flatMap(resource -> delete(resource, resourceResolver))
                    .or(() -> {
                        log.trace("Resource at {} not found and won't be deleted", jcrPath);
                        return Optional.empty();
                    });
        }
    }

    private Optional<JCRPath> delete(Resource resourceToBeDeleted, ResourceResolver resourceResolver) {
        log.trace("Deleting {}", resourceToBeDeleted);
        TargetJCRPath jcrPathToDelete = new TargetJCRPath(resourceToBeDeleted);
        try {
            resourceResolver.delete(resourceToBeDeleted);
            resourceResolver.commit();
            log.trace("Deleted {}", jcrPath);
            return Optional.of(jcrPathToDelete);
        } catch (PersistenceException exception) {
            String message = "Unable to delete %s".formatted(jcrPathToDelete);
            log.error(message, exception);
            return Optional.empty();
        }
    }

    @Override
    public List<String> requiredPrivileges() {
        return List.of(
                PrivilegeConstants.JCR_READ,
                PrivilegeConstants.JCR_REMOVE_CHILD_NODES,
                PrivilegeConstants.JCR_REMOVE_NODE
        );
    }
}
