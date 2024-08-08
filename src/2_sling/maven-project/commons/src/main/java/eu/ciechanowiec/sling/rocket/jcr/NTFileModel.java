package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
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
import java.util.Optional;

@Model(
        adaptables = Resource.class,
        adapters = NTFile.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.REQUIRED
)
@Slf4j
@ToString
@SuppressWarnings("pR")
class NTFileModel implements NTFile {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    @Inject
    NTFileModel(@Self Resource resource, @OSGiService ResourceAccess resourceAccess) {
        String resourcePath = resource.getPath();
        this.jcrPath = new TargetJCRPath(resourcePath);
        this.resourceAccess = resourceAccess;
        log.trace("Initialized {}", this);
    }

    @Override
    @SuppressWarnings({"ReturnCount", "MethodWithMultipleReturnPoints"})
    public Optional<File> retrieve() {
        log.trace("Retrieving a file from {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        boolean isNTFile = nodeProperties.isPrimaryType(JcrConstants.NT_FILE);
        if (!isNTFile) {
            log.debug("Invalid primary type of {}. Expected: '{}'", this, JcrConstants.NT_FILE);
            return Optional.empty();
        }
        JCRPath jcrContentChildJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath), JcrConstants.JCR_CONTENT);
        NodeProperties jcrContentChildNP = new NodeProperties(jcrContentChildJCRPath, resourceAccess);
        boolean isJCRContentChildNTResource = jcrContentChildNP.isPrimaryType(JcrConstants.NT_RESOURCE);
        if (!isJCRContentChildNTResource) {
            log.debug(
                    "Child node '{}' of {} has invalid primary type. Expected: '{}'",
                    JcrConstants.JCR_CONTENT, this, JcrConstants.NT_RESOURCE
            );
            return Optional.empty();
        }
        return jcrContentChildNP.retrieveFile(JcrConstants.JCR_DATA);
    }
}
