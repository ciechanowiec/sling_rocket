package eu.ciechanowiec.sling.rocket.jcr.query;

import lombok.SneakyThrows;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import java.util.Optional;

/**
 * {@link ResourceResolver} with wrapped access to the {@link QueryManager}.
 */
@SuppressWarnings({"TypeName", "WeakerAccess"})
public class WithQueryManager {

    private final ResourceResolver resourceResolver;

    /**
     * Constructs an instance of this class.
     *
     * @param resourceResolver {@link ResourceResolver} that will be wrapped by this {@link WithQueryManager}
     */
    @SuppressWarnings("WeakerAccess")
    public WithQueryManager(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    /**
     * Returns a {@link QueryManager} for the wrapped {@link ResourceResolver}.
     *
     * @return {@link QueryManager} for the wrapped {@link ResourceResolver}
     */
    @SneakyThrows
    public QueryManager get() {
        Session session = Optional.ofNullable(resourceResolver.adaptTo(Session.class)).orElseThrow();
        return session.getWorkspace().getQueryManager();
    }
}
