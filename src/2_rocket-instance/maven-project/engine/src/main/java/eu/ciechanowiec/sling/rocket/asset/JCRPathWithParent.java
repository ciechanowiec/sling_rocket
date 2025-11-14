package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Optional;

@ToString
@Slf4j
class JCRPathWithParent implements JCRPath {

    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    JCRPathWithParent(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
        log.trace("Initialized {}", this);
    }

    JCRPathWithParent(WithJCRPath withJCRPath, ResourceAccess resourceAccess) {
        this(withJCRPath.jcrPath(), resourceAccess);
    }

    Optional<ParentJCRPath> parent() {
        log.trace("Retrieving parent JCR path for {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                .map(resource -> Optional.ofNullable(resource.getParent()).orElse(resource))
                .map(Resource::getPath)
                .map(parentPath -> new ParentJCRPath(new TargetJCRPath(parentPath)));
        }
    }

    @Override
    public String get() {
        return jcrPath.get();
    }
}
