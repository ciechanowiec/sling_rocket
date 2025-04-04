package eu.ciechanowiec.sling.rocket.privilege;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthID;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.SneakyThrows;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Optional;

/**
 * Administrator of {@link Privilege}s.
 */
@SuppressWarnings("WeakerAccess")
public class PrivilegeAdmin {

    private final FullResourceAccess fullResourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} that will be used to acquire access to the resources
     */
    public PrivilegeAdmin(FullResourceAccess fullResourceAccess) {
        this.fullResourceAccess = fullResourceAccess;
    }

    /**
     * Shortcut for {@link AccessControlUtils#allow(Node, String, String...)}.
     *
     * @param jcrPath    {@link JCRPath} to the {@link Node} to which the {@link Privilege}s should be granted
     * @param authID     {@link AuthID} for which the {@link Privilege}s should be granted
     * @param privileges names of the {@link Privilege}s that should be granted; usage of {@link PrivilegeConstants}
     *                   fields is recommended
     * @return {@code true} if the operation was successful; {@code false} otherwise, i.a. when the specified
     * {@link Privilege}s are already granted
     */
    @SneakyThrows
    public boolean allow(JCRPath jcrPath, AuthID authID, String... privileges) {
        return set(jcrPath, authID, true, privileges);
    }

    /**
     * Shortcut for {@link AccessControlUtils#deny(Node, String, String...)}.
     *
     * @param jcrPath    {@link JCRPath} to the {@link Node} to which the {@link Privilege}s should be denied
     * @param authID     {@link AuthID} for which the {@link Privilege}s should be denied
     * @param privileges names of the {@link Privilege}s that should be denied; usage of {@link PrivilegeConstants}
     *                   fields is recommended
     * @return {@code true} if the operation was successful; {@code false} otherwise, i.a. when the specified
     * {@link Privilege}s are already denied
     */
    @SneakyThrows
    public boolean deny(JCRPath jcrPath, AuthID authID, String... privileges) {
        return set(jcrPath, authID, false, privileges);
    }

    @SneakyThrows
    private boolean set(JCRPath jcrPath, AuthID authID, boolean doAllow, String... privileges) {
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            Session session = Optional.ofNullable(resourceResolver.adaptTo(Session.class)).orElseThrow();
            JackrabbitSession jackSession = (JackrabbitSession) session;
            UserManager userManager = jackSession.getUserManager();
            Node node = session.getNode(jcrPath.get());
            boolean result = Optional.ofNullable(userManager.getAuthorizable(authID.get()))
                .map(SneakyFunction.sneaky(Authorizable::getPrincipal))
                .map(Principal::getName)
                .map(SneakyFunction.sneaky(principalName -> {
                    if (doAllow) {
                        return AccessControlUtils.allow(node, principalName, privileges);
                    } else {
                        return AccessControlUtils.deny(node, principalName, privileges);
                    }
                }))
                .orElse(false);
            session.save();
            return result;
        }
    }
}
