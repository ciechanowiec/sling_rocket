package eu.ciechanowiec.sling.rocket.privilege;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;

import javax.jcr.Repository;
import javax.jcr.security.Privilege;
import java.util.List;

/**
 * Represents an entity that requires from the {@link User} to have assigned {@link Privilege}s listed in
 * {@link RequiresPrivilege#requiredPrivileges()} in order to interact with the {@link Repository} via this entity.
 */
@FunctionalInterface
public interface RequiresPrivilege {

    /**
     * List all {@link Privilege}s that the {@link User} must have assigned in order to interact with
     * the {@link Repository} via this {@link RequiresPrivilege} entity. Every listed {@link Privilege}
     * is guaranteed to be one of {@link PrivilegeConstants} fields. Empty {@link List} is returned if
     * no {@link Privilege}s are required.
     * @return all {@link Privilege}s that the {@link User} must have assigned in order to interact with
     *         the {@link Repository} via this {@link RequiresPrivilege} entity;
     *         every listed {@link Privilege} is guaranteed to be one of {@link PrivilegeConstants} fields;
     *         empty {@link List} is returned if no {@link Privilege}s are required
     */
    List<String> requiredPrivileges();
}
