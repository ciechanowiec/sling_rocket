package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.NotReferencableException;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;

@ToString
@Slf4j
class BasicReferencable implements Referencable {

    private final WithJCRPath withJCRPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    BasicReferencable(WithJCRPath withJCRPath, ResourceAccess resourceAccess) {
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
