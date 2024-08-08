package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;

@Model(
        adaptables = Resource.class,
        adapters = AssetMetadata.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.REQUIRED
)
@Slf4j
@ToString
@SuppressWarnings("pR")
class AssetMetadataModel implements AssetMetadata, WithJCRPath {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    @Inject
    AssetMetadataModel(@Self Resource resource, @OSGiService ResourceAccess resourceAccess) {
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
    public NodeProperties retrieve() {
        return new NodeProperties(this, resourceAccess);
    }
}
