package eu.ciechanowiec.sling.rocket.auth;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Virtual {@link Resource} that acts as API for user logouts.
 */
@Slf4j
@ToString
@Component(
    service = {ResourceProvider.class, LogoutAPI.class},
    property = {
        ResourceProvider.PROPERTY_ROOT + "=" + LogoutAPI.LOGOUT_API_PATH,
        ResourceProvider.PROPERTY_NAME + "=" + "LogoutAPI",
        ResourceProvider.PROPERTY_AUTHENTICATE + "=" + ResourceProvider.AUTHENTICATE_REQUIRED,
        ResourceProvider.PROPERTY_MODIFIABLE + "=" + "false",
        ResourceProvider.PROPERTY_REFRESHABLE + "=" + "true"
    },
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = LogoutAPIConfig.class)
@ServiceDescription("Virtual Resource that acts as API for user logouts")
public class LogoutAPI extends ResourceProvider<Object> {

    /**
     * Path of the Logout API.
     */
    @SuppressWarnings({"squid:S1075", "WeakerAccess"})
    public static final String LOGOUT_API_PATH = "/api/logout";

    /**
     * Type of virtual {@link Resource}s that acts as API for user logouts.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String LOGOUT_API_RESOURCE_TYPE = "api/logout";

    private LogoutAPIConfig config;
    private final Map<String, BiFunction<String, ResourceResolver, Resource>> resourcesByPaths;

    /**
     * Constructs an instance of this class.
     *
     * @param config {@link LogoutAPIConfig} used by the constructed instance
     */
    @Activate
    public LogoutAPI(LogoutAPIConfig config) {
        this.config = config;
        this.resourcesByPaths = Map.of(
            LOGOUT_API_PATH, (path, resourceResolver) -> {
                log.trace("Providing SyntheticResource for path: '{}'", path);
                return new SyntheticResource(
                    resourceResolver, LOGOUT_API_PATH, LOGOUT_API_RESOURCE_TYPE
                );
            }
        );
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(LogoutAPIConfig config) {
        this.config = config;
        log.info("Configured {}", this);
    }

    @Override
    public Resource getResource(
        ResolveContext<Object> ctx, @NotNull String path,
        @NotNull ResourceContext resourceContext, @Nullable Resource parent
    ) {
        ResourceResolver resourceResolver = ctx.getResourceResolver();
        return Optional.ofNullable(resourcesByPaths.get(path))
            .filter(resourceSupplier -> config.is$_$enabled())
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
    public Iterator<Resource> listChildren(@NotNull ResolveContext<Object> ctx, @NotNull Resource parent) {
        return Collections.emptyIterator();
    }
}
