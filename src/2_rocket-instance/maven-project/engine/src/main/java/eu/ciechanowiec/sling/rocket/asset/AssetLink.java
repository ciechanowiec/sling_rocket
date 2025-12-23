package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.ReferenceProperty;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.ref.Referenceable;
import eu.ciechanowiec.sling.rocket.jcr.ref.ReferenceableSimple;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Optional;

@Slf4j
@ToString
class AssetLink implements Asset {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    AssetLink(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
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
                        .map(resource -> new UniversalAsset(resource, resourceAccess));
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
        Referenceable referenceable = new ReferenceableSimple(this, resourceAccess);
        return referenceable.jcrUUID();
    }
}
