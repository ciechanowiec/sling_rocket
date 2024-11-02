package eu.ciechanowiec.sling.rocket.commons;

import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Repository;

/**
 * Provides full and unlimited access to Apache Sling resources, including the underlying {@link Repository}.
 */
@FunctionalInterface
public interface ResourceAccess {

    /**
     * Provides full and unlimited access to Apache Sling resources, including the underlying {@link Repository}.
     * @return {@link ResourceResolver} that provides the resource access
     */
    ResourceResolver acquireAccess();
}
