package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.ReferenceProperty;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import java.util.Optional;

@Model(
        adaptables = Resource.class,
        adapters = Asset.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.REQUIRED
)
@Slf4j
@ToString
@SuppressWarnings("pR")
class AssetLinkModel implements Asset {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    @Inject
    AssetLinkModel(@Self Resource resource, @OSGiService ResourceAccess resourceAccess) {
        String resourcePath = resource.getPath();
        this.jcrPath = new TargetJCRPath(resourcePath);
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        log.trace("Initialized {}", this);
    }

    @Override
    public AssetFile assetFile() {
        Asset linkedAsset = linkedAsset();
        log.trace("For {} this linked asset was resolved: {}", this, linkedAsset);
        return linkedAsset.assetFile();
    }

    @Override
    public AssetMetadata assetMetadata() {
        Asset linkedAsset = linkedAsset();
        log.trace("For {} this linked asset was resolved: {}", this, linkedAsset);
        return linkedAsset.assetMetadata();
    }

    private Asset linkedAsset() {
        ReferenceProperty referenceProperty = new ReferenceProperty(jcrPath, PN_LINKED_ASSET, resourceAccess);
        Optional<JCRPath> referencedNodeJCRPathNullable = referenceProperty.referencedNode();
        log.trace("{} has this linked asset: {}", this, referencedNodeJCRPathNullable);
        return referencedNodeJCRPathNullable.flatMap(
                referencedNodeJCRPath -> {
                    String referencedNodeJCRPathRaw = referencedNodeJCRPath.get();
                    try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                        return Optional.ofNullable(resourceResolver.getResource(referencedNodeJCRPathRaw))
                                       .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Asset.class)));
                    }
                }
        ).orElseThrow();
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_ASSET_LINK);
    }

    @Override
    public String jcrUUID() {
        Referencable referencable = new BasicReferencable(this, resourceAccess);
        return referencable.jcrUUID();
    }
}
