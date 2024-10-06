package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import jakarta.ws.rs.core.MediaType;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;

import java.util.Map;
import java.util.Optional;

@Slf4j
@ToString
class AssetMetadataBasic implements AssetMetadata, WithJCRPath {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    AssetMetadataBasic(Resource resource, ResourceAccess resourceAccess) {
        this.resourceAccess = resourceAccess;
        this.jcrPath = new TargetJCRPath(resource.getPath());
        assertPrimaryType();
        log.trace("Initialized {}", this);
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(Asset.NT_ASSET_METADATA);
    }

    @Override
    public Optional<NodeProperties> properties() {
        return Optional.of(new NodeProperties(this, resourceAccess));
    }

    @Override
    public String mimeType() {
        return properties().orElseThrow().propertyValue(PN_MIME_TYPE, MediaType.WILDCARD);
    }

    @Override
    public Map<String, String> all() {
        return properties().orElseThrow().all();
    }
}
