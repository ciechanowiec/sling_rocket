package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.NTFile;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import jakarta.ws.rs.core.MediaType;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;
import java.util.Optional;

@Model(
        adaptables = Resource.class,
        adapters = {NTFile.class, Asset.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.REQUIRED
)
@Slf4j
@ToString
@SuppressWarnings("pR")
class NTFileModel implements NTFile, Asset {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    @Inject
    NTFileModel(@Self Resource resource, @OSGiService ResourceAccess resourceAccess) {
        String resourcePath = resource.getPath();
        this.jcrPath = new TargetJCRPath(resourcePath);
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        assertContentChildNodeType();
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
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(JcrConstants.NT_FILE);
    }

    private void assertContentChildNodeType() {
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        log.trace("Asserting primary type of {}", jcrContentChildJCRPath);
        NodeProperties jcrContentChildNP = new NodeProperties(jcrContentChildJCRPath, resourceAccess);
        jcrContentChildNP.assertPrimaryType(JcrConstants.NT_RESOURCE);
    }

    @Override
    public AssetFile assetFile() {
        return this::retrieve;
    }

    @Override
    public AssetMetadata assetMetadata() {
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        NodeProperties jcrContentChildNP = new NodeProperties(jcrContentChildJCRPath, resourceAccess);
        return new AssetMetadata() {
            @Override
            public String mimeType() {
                return properties().flatMap(
                                nodeProperties -> nodeProperties.propertyValue(
                                        JcrConstants.JCR_MIMETYPE, DefaultProperties.STRING_CLASS
                                )
                        ).orElse(MediaType.WILDCARD);
            }

            @Override
            public Map<String, String> all() {
                return properties().map(NodeProperties::all).orElse(Map.of());
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.of(jcrContentChildNP);
            }
        };
    }

    @Override
    public String jcrUUID() {
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        Referencable referencable = new BasicReferencable(() -> jcrContentChildJCRPath, resourceAccess);
        return referencable.jcrUUID();
    }
}
