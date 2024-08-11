package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.Referencable;
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
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@Model(
        adaptables = Resource.class,
        adapters = Assets.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.REQUIRED
)
@Slf4j
@ToString
@SuppressWarnings("pR")
class AssetsModel implements Assets {

    @Getter
    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    @Inject
    AssetsModel(@Self Resource resource, @OSGiService ResourceAccess resourceAccess) {
        String resourcePath = resource.getPath();
        this.jcrPath = new TargetJCRPath(resourcePath);
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
        log.trace("Initialized {}", this);
    }

    @Override
    public Collection<Asset> get() {
        log.trace("Retrieving assets from '{}'", jcrPath);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                           .map(Resource::getChildren)
                           .map(UnwrappedIteration::new)
                           .map(UnwrappedIteration::stream)
                           .orElseGet(Stream::empty)
                           .map(resource -> Optional.ofNullable(resource.adaptTo(Asset.class)))
                           .flatMap(Optional::stream)
                           .toList();
        }
    }

    @Override
    public String jcrUUID() {
        Referencable referencable = new BasicReferencable(this, resourceAccess);
        return referencable.jcrUUID();
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_ASSETS);
    }
}
