package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;

import javax.jcr.Node;

/**
 * Basic implementation of {@link Referencable}.
 */
@ToString
@Slf4j
public class BasicReferencable implements Referencable {

    private final WithJCRPath withJCRPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     * @param withJCRPath object that contains a {@link JCRPath} to the underlying {@link Node}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                       object to acquire access to resources
     */
    public BasicReferencable(WithJCRPath withJCRPath, ResourceAccess resourceAccess) {
        this.withJCRPath = withJCRPath;
        this.resourceAccess = resourceAccess;
        log.trace("Initialized {}", this);
    }

    @Override
    public String jcrUUID() {
        NodeProperties nodeProperties = new NodeProperties(withJCRPath, resourceAccess);
        return nodeProperties.propertyValue(JcrConstants.JCR_UUID, DefaultProperties.STRING_CLASS).orElseThrow(() -> {
            String message = String.format("Lacking %s property. Not referencable: %s", JcrConstants.JCR_UUID, this);
            return new NotReferencableException(message);
        });
    }
}
