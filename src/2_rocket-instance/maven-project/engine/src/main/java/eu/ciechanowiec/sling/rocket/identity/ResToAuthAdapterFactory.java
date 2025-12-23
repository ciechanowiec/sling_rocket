package eu.ciechanowiec.sling.rocket.identity;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import java.util.Optional;

/**
 * {@link AdapterFactory} that adapts a {@link Resource} to an {@link Authorizable} based on the
 * {@link Resource#getPath()}.
 */
@Component(
    service = {AdapterFactory.class, ResToAuthAdapterFactory.class},
    property = {
        AdapterFactory.ADAPTER_CLASSES + "=org.apache.jackrabbit.api.security.user.Authorizable",
        AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.Resource"
    }
)
@Slf4j
public class ResToAuthAdapterFactory implements AdapterFactory {

    /**
     * Constructs an instance of this class.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public ResToAuthAdapterFactory() {
        // no special initialization
    }

    @Override
    @SuppressWarnings({"MatchXpath", "NullableProblems"})
    public <T> T getAdapter(Object adaptable, Class<T> type) {
        return Optional.of(adaptable)
            .filter(Resource.class::isInstance)
            .map(Resource.class::cast)
            .map(resource -> getAdapter(resource, type))
            .orElse(null);
    }

    @SneakyThrows
    @SuppressWarnings("PMD.CloseResource")
    private <T> T getAdapter(Resource resource, Class<T> type) {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        String resourcePath = resource.getPath();
        UserManager userManager = new WithUserManager(resourceResolver).get();
        return type.cast(userManager.getAuthorizableByPath(resourcePath));
    }
}
