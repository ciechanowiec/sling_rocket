package eu.ciechanowiec.sling.rocket.jcr.ref;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;

import javax.jcr.Node;
import javax.jcr.Property;

/**
 * {@link Referenceable} that simply retrieves the value of the {@link Property} named {@link JcrConstants#JCR_UUID} of
 * type {@link String} directly from the underlying {@link Node}.
 */
@ToString
@Slf4j
public class ReferenceableSimple implements Referenceable {

    private final WithJCRPath withJCRPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param withJCRPath    object that contains a {@link JCRPath} to the {@link Node} that has the {@link Property}
     *                       named {@link JcrConstants#JCR_UUID} of type {@link String}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     */
    public ReferenceableSimple(WithJCRPath withJCRPath, ResourceAccess resourceAccess) {
        this.withJCRPath = withJCRPath;
        this.resourceAccess = resourceAccess;
        log.trace("Initialized {}", this);
    }

    @Override
    public String jcrUUID() {
        NodeProperties nodeProperties = new NodeProperties(withJCRPath, resourceAccess);
        return nodeProperties.propertyValue(JcrConstants.JCR_UUID, DefaultProperties.STRING_CLASS).orElseThrow(
            () -> {
                String message = "Lacking %s property. Not referenceable: %s".formatted(
                    JcrConstants.JCR_UUID, this
                );
                return new NotReferenceableException(message);
            }
        );
    }
}
