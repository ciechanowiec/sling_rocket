package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.Optional;

/**
 * Represents a {@link Property} of type {@link PropertyType#WEAKREFERENCE}, {@link PropertyType#REFERENCE} or
 * {@link PropertyType#PATH}.
 */
@Slf4j
@ToString
public class ReferenceProperty {

    private final JCRPath jcrPathToNode;
    private final String propertyName;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param jcrPathToNode  {@link JCRPath} to a {@link Node} that contains the {@link Property} represented by the
     *                       constructed object
     * @param propertyName   name of the {@link Property} represented by the constructed object; the {@link Property}
     *                       must be of type {@link PropertyType#WEAKREFERENCE}, {@link PropertyType#REFERENCE} or
     *                       {@link PropertyType#PATH}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     */
    public ReferenceProperty(JCRPath jcrPathToNode, String propertyName, ResourceAccess resourceAccess) {
        this.jcrPathToNode = jcrPathToNode;
        this.propertyName = propertyName;
        this.resourceAccess = resourceAccess;
        log.trace("Initialized {}", this);
    }

    /**
     * Returns an {@link Optional} containing the {@link JCRPath} to the {@link Node} referenced by the {@link Property}
     * represented by this object.
     *
     * @return {@link Optional} containing the {@link JCRPath} to the {@link Node} referenced by the {@link Property}
     * represented by this object; empty {@link Optional} is returned if the referenced {@link Node} can't be
     * identified
     */
    public Optional<JCRPath> referencedNode() {
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathToNodeRaw = jcrPathToNode.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathToNodeRaw))
                .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
                .flatMap(node -> new ConditionalProperty(propertyName).retrieveFrom(node))
                .flatMap(this::referencedNode);
        }
    }

    @SneakyThrows
    @SuppressWarnings("OverlyBroadCatchBlock")
    private Optional<JCRPath> referencedNode(Property referencingProperty) {
        log.trace("Getting the referenced node for {} and {}", this, referencingProperty);
        try {
            Node referencedNode = referencingProperty.getNode();
            log.trace("For {} this referenced node detected: {}", this, referencedNode);
            String referencedPath = referencedNode.getPath();
            return Optional.of(new TargetJCRPath(referencedPath));
        } catch (RepositoryException exception) {
            String message = String.format(
                "Unable to get the referenced node for %s and %s", this, referencingProperty
            );
            log.warn(message, exception);
            return Optional.empty();
        }
    }
}
