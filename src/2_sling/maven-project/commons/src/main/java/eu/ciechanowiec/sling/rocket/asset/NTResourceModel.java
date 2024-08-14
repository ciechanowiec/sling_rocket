package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NTFile;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import java.io.File;
import java.util.Optional;

@Model(
        adaptables = Resource.class,
        adapters = Asset.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.REQUIRED
)
@Slf4j
@ToString
@SuppressWarnings("pR")
class NTResourceModel implements NTFile, Asset {

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    @Inject
    NTResourceModel(@Self Resource resource, @OSGiService ResourceAccess resourceAccess) {
        String resourcePath = resource.getPath();
        this.jcrPath = new TargetJCRPath(resourcePath);
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        assertParentNodeType();
        log.trace("Initialized {}", this);
    }

    @Override
    public Optional<File> retrieve() {
        log.trace("Retrieving a file from {}", this);
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        NodeProperties jcrContentChildNP = new NodeProperties(jcrContentChildJCRPath, resourceAccess);
        return jcrContentChildNP.retrieveFile(JcrConstants.JCR_DATA);
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(jcrPath, resourceAccess);
        nodeProperties.assertPrimaryType(JcrConstants.NT_RESOURCE);
    }

    private void assertParentNodeType() {
        ParentJCRPath parentJCRPath = parentPath();
        log.trace("Asserting primary type of {}", parentJCRPath);
        NodeProperties parentNP = new NodeProperties(parentJCRPath, resourceAccess);
        parentNP.assertPrimaryType(JcrConstants.NT_FILE);
    }

    private ParentJCRPath parentPath() {
        return new JCRPathWithParent(jcrPath, resourceAccess).parent().orElseThrow();
    }

    private Asset parentAsset() {
        ParentJCRPath parentJCRPath = parentPath();
        log.trace("Parent JCR path for {} is {}", this, parentJCRPath);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String parentJCRPathRaw = parentJCRPath.get();
            return Optional.ofNullable(resourceResolver.getResource(parentJCRPathRaw))
                           .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Asset.class)))
                           .orElseThrow();
        }
    }

    @Override
    public AssetFile assetFile() {
        return parentAsset().assetFile();
    }

    @Override
    public AssetMetadata assetMetadata() {
        return parentAsset().assetMetadata();
    }

    @Override
    public String jcrUUID() {
        return parentAsset().jcrUUID();
    }

    @Override
    public JCRPath jcrPath() {
        return parentAsset().jcrPath();
    }
}
