package eu.ciechanowiec.sling.rocket.commons;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Repository;

/**
 * Provides access to Apache Sling resources, including the underlying {@link Repository}.
 * <p>
 * The scope of the access is defined by the implementation. Among others, implementations may:
 * <ol>
 *     <li>Provide full and unlimited access (e.g. {@link FullResourceAccess}).</li>
 *     <li>Provide access as it is defined for a specific {@link User} (e.g. {@link UserResourceAccess}).</li>
 * </ol>
 */
@FunctionalInterface
public interface ResourceAccess {

    /**
     * Provides access to Apache Sling resources, including the underlying {@link Repository}.
     * @return {@link ResourceResolver} that provides the resource access
     */
    ResourceResolver acquireAccess();
}
