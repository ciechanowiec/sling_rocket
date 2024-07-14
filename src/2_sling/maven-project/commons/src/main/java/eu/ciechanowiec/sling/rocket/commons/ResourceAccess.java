package eu.ciechanowiec.sling.rocket.commons;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * Provides full and unlimited access to Apache Sling resources, including the underlying Java Content Repository.
 */
@FunctionalInterface
public interface ResourceAccess {

    /**
     * Provides full and unlimited access to Apache Sling resources, including the underlying Java Content Repository.
     * @return {@link ResourceResolver} that provides the resource access
     */
    ResourceResolver acquireAccess();
}
