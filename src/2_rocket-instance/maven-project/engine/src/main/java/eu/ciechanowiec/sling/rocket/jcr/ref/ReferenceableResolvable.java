package eu.ciechanowiec.sling.rocket.jcr.ref;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static eu.ciechanowiec.sneakyfun.SneakyFunction.sneaky;

/**
 * {@link Referenceable} that can be resolved to an actual {@link Node} or a {@link Resource} linking that
 * {@link Node}.
 */
@Slf4j
@SuppressWarnings("WeakerAccess")
public class ReferenceableResolvable implements Referenceable {

    private final Referenceable referenceable;
    private final Supplier<Session> sessionSupplier;
    private final Supplier<ResourceResolver> resourceResolverSupplier;

    /**
     * Constructs an instance of this class.
     *
     * @param referenceable    {@link Referenceable} that should be resolved to an actual {@link Node} or a
     *                         {@link Resource} linking that {@link Node}
     * @param resourceResolver {@link ResourceResolver} that will be used by the constructed object to acquire access to
     *                         the {@link Repository}
     */
    public ReferenceableResolvable(Referenceable referenceable, ResourceResolver resourceResolver) {
        this.referenceable = referenceable;
        this.sessionSupplier = () -> Optional.ofNullable(resourceResolver.adaptTo(Session.class)).orElseThrow();
        this.resourceResolverSupplier = () -> resourceResolver;
    }

    /**
     * Constructs an instance of this class.
     *
     * @param referenceable {@link Referenceable} that should be resolved to an actual {@link Node} or a
     *                      {@link Resource} linking that {@link Node}
     * @param session       {@link Session} that will be used by the constructed object to acquire access to the
     *                      {@link Repository}
     */
    public ReferenceableResolvable(Referenceable referenceable, Session session) {
        this.referenceable = referenceable;
        this.sessionSupplier = () -> session;
        this.resourceResolverSupplier = () -> toResourceResolver(session);
    }

    @SneakyThrows
    private ResourceResolver toResourceResolver(Session session) {
        BundleContext bundleContext = Optional.ofNullable(FrameworkUtil.getBundle(ReferenceableResolvable.class))
            .map(Bundle::getBundleContext)
            .orElseThrow();
        ServiceReference<ResourceResolverFactory> resourceResolverFactorySS = Optional.ofNullable(
            bundleContext.getServiceReference(ResourceResolverFactory.class)
        ).orElseThrow();
        ResourceResolverFactory resourceResolverFactory = Optional.ofNullable(
            bundleContext.getService(resourceResolverFactorySS)
        ).orElseThrow();
        try {
            Map<String, Object> authInfo = new ConcurrentHashMap<>();
            authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
            return resourceResolverFactory.getResourceResolver(authInfo);
        } finally {
            bundleContext.ungetService(resourceResolverFactorySS);
        }
    }

    /**
     * Retrieves the {@link Resource} that links the {@link Node} represented by this {@link Referenceable}.
     *
     * @return {@link Optional} containing the retrieved {@link Resource}; an empty {@link Optional} is returned if the
     * {@link Resource} cannot be resolved
     */
    public Optional<Resource> resource() {
        String uuid = jcrUUID();
        return node().map(sneaky(Node::getPath))
            .flatMap(
                nodePath -> Optional.ofNullable(
                    resourceResolverSupplier.get().getResource(nodePath)
                )
            ).map(
                resource -> {
                    log.trace("By UUID '{}' this resource resolved: {}", uuid, resource);
                    return resource;
                }
            );
    }

    /**
     * Retrieves the {@link Node} represented by this {@link Referenceable}.
     *
     * @return {@link Optional} containing the retrieved {@link Node}; an empty {@link Optional} is returned if the
     * {@link Node} cannot be resolved
     */
    @SuppressWarnings("OverlyBroadCatchBlock")
    public Optional<Node> node() {
        String uuid = jcrUUID();
        try {
            Node node = sessionSupplier.get().getNodeByIdentifier(uuid);
            log.trace("By UUID '{}' this node resolved: {}", uuid, node);
            return Optional.of(node);
        } catch (RepositoryException exception) {
            log.debug("Unable to resolve node by UUID: '%s'".formatted(uuid), exception);
            return Optional.empty();
        }
    }

    @Override
    public String jcrUUID() {
        return referenceable.jcrUUID();
    }
}
