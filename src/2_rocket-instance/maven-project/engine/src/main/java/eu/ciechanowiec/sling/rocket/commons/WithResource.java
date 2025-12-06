package eu.ciechanowiec.sling.rocket.commons;

import org.apache.sling.api.resource.Resource;

/**
 * Entity that has an associated {@link Resource}.
 */
@SuppressWarnings({"unused", "InterfaceNeverImplemented"})
@FunctionalInterface
public interface WithResource {

    /**
     * Returns the {@link Resource} associated with this entity.
     *
     * @return {@link Resource} associated with this entity
     */
    Resource resource();
}
