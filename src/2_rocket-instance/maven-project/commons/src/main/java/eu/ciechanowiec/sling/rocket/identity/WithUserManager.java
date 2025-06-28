package eu.ciechanowiec.sling.rocket.identity;

import lombok.SneakyThrows;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Session;
import java.util.Optional;

/**
 * {@link ResourceResolver} with a wrapped access to {@link UserManager}.
 */
@SuppressWarnings("TypeName")
public class WithUserManager {

    private final ResourceResolver resourceResolver;

    /**
     * Constructs an instance of this class.
     *
     * @param resourceResolver {@link ResourceResolver} that will be wrapped by this {@link WithUserManager}
     */
    public WithUserManager(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    /**
     * Returns a {@link UserManager} for the wrapped {@link ResourceResolver}.
     *
     * @return {@link UserManager} for the wrapped {@link ResourceResolver}
     */
    @SneakyThrows
    public UserManager get() {
        Session session = Optional.ofNullable(resourceResolver.adaptTo(Session.class)).orElseThrow();
        JackrabbitSession jackSession = (JackrabbitSession) session;
        return jackSession.getUserManager();
    }
}
