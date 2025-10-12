package eu.ciechanowiec.sling.rocket.favicon;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Virtual {@link Resource} that acts as API for favicon requests.
 */
@Slf4j
@ToString
@Component(
    service = {ResourceProvider.class, FaviconAPI.class},
    property = {
        ResourceProvider.PROPERTY_ROOT + "=" + FaviconAPI.FAVICON_PATH,
        ResourceProvider.PROPERTY_NAME + "=" + "FaviconAPI",
        ResourceProvider.PROPERTY_AUTHENTICATE + "=" + ResourceProvider.AUTHENTICATE_REQUIRED,
        ResourceProvider.PROPERTY_MODIFIABLE + "=" + "false",
        ResourceProvider.PROPERTY_REFRESHABLE + "=" + "true"
    },
    immediate = true
)
@ServiceDescription("Virtual Resource that acts as API for favicon requests")
public class FaviconAPI extends ResourceProvider<Object> {

    /**
     * Path of the Favicon API.
     */
    @SuppressWarnings({"squid:S1075", "WeakerAccess"})
    public static final String FAVICON_PATH = "/favicon.ico";

    /**
     * Type of virtual {@link Resource}s that acts as API for favicon requests.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String FAVICON_RESOURCE_TYPE = "rocket/favicon";

    private final Map<String, BiFunction<String, ResourceResolver, Resource>> resourcesByPaths;

    /**
     * Constructs an instance of this class.
     */
    @Activate
    public FaviconAPI() {
        this.resourcesByPaths = Map.of(
            FAVICON_PATH, (path, resourceResolver) -> {
                log.trace("Providing SyntheticResource for path: '{}'", path);
                return new SyntheticResource(
                    resourceResolver, FAVICON_PATH, FAVICON_RESOURCE_TYPE
                );
            }
        );
        log.info("Initialized {}", this);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public Resource getResource(
        ResolveContext<Object> ctx, String path, ResourceContext resourceContext, Resource parent
    ) {
        ResourceResolver resourceResolver = ctx.getResourceResolver();
        return Optional.ofNullable(resourcesByPaths.get(path))
            .map(resourceSupplier -> resourceSupplier.apply(path, resourceResolver))
            .orElseGet(
                () -> {
                    log.trace("Providing NonExistingResource for path: '{}'", path);
                    return new NonExistingResource(resourceResolver, Resource.RESOURCE_TYPE_NON_EXISTING);
                }
            );
    }

    @Override
    @Nullable
    @SuppressWarnings("NullableProblems")
    public Iterator<Resource> listChildren(
        ResolveContext<Object> ctx,
        Resource parent
    ) {
        return Collections.emptyIterator();
    }
}
