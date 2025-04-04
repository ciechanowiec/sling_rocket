package eu.ciechanowiec.sling.rocket.asset;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import lombok.ToString;

import java.util.Optional;

@ToString
class AssetRealCape implements Asset {

    private final NTFile ntFile;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    AssetRealCape(NTFile ntFile, ResourceAccess resourceAccess) {
        this.ntFile = ntFile;
        this.resourceAccess = resourceAccess;
    }

    private Optional<AssetReal> wearCape() {
        return new JCRPathWithParent(ntFile, resourceAccess).parent()
            .map(parentJCRPath -> new NodeProperties(parentJCRPath, resourceAccess))
            .filter(parentNodeProperties -> parentNodeProperties.isPrimaryType(NT_ASSET_REAL))
            .map(parentNodeProperties -> new AssetReal(parentNodeProperties, resourceAccess));
    }

    @Override
    public AssetFile assetFile() {
        return wearCape().map(AssetReal::assetFile).orElse(ntFile.assetFile());
    }

    @Override
    public AssetMetadata assetMetadata() {
        return wearCape().map(AssetReal::assetMetadata).orElse(ntFile.assetMetadata());
    }

    @Override
    public String jcrUUID() {
        return wearCape().map(AssetReal::jcrUUID).orElse(ntFile.jcrUUID());
    }

    @Override
    public JCRPath jcrPath() {
        return wearCape().map(AssetReal::jcrPath).orElse(ntFile.jcrPath());
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @SuppressFBWarnings("EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS")
    public boolean equals(Object comparedObject) {
        return wearCape().map(assetReal -> assetReal.equals(comparedObject)).orElse(ntFile.equals(comparedObject));
    }

    @Override
    public int hashCode() {
        return wearCape().map(AssetReal::hashCode).orElse(ntFile.hashCode());
    }
}
