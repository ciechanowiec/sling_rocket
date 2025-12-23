package eu.ciechanowiec.sling.rocket.identity.creation;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthID;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUniversal;
import eu.ciechanowiec.sling.rocket.identity.WithUserManager;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import javax.jcr.Node;
import javax.jcr.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sends an {@link Event} to the {@link AuthCreationBroadcast#TOPIC_AUTH_CREATION} topic upon creation and persisting in
 * the {@link Repository} of every {@link Authorizable}. The {@link Event} can be adapted to {@link AuthCreated} via
 * {@link AuthCreated#AuthCreated(Event)} to retrieve the {@link AuthID} of the created {@link Authorizable}.
 */
@Component(
    service = {AuthCreationBroadcast.class, ResourceChangeListener.class},
    immediate = true,
    property = {
        ResourceChangeListener.PATHS + "=" + "/home/users",
        ResourceChangeListener.PATHS + "=" + "/home/groups",
        ResourceChangeListener.CHANGES + "=" + ResourceChangeListener.CHANGE_ADDED
    }
)
@ServiceDescription("Sends an OSGi Event upon creation and persisting in the Repository of every Authorizable")
@Slf4j
public class AuthCreationBroadcast implements ResourceChangeListener {

    /**
     * The topic for the {@link Event} which is sent upon creation and persisting in the {@link Repository} of every
     * {@link Authorizable}. The {@link Event} can be adapted to {@link AuthCreated} via
     * {@link AuthCreated#AuthCreated(Event)} to retrieve the {@link AuthID} of the created {@link Authorizable}.
     */
    public static final String TOPIC_AUTH_CREATION
        = "eu/ciechanowiec/sling/rocket/identity/creation/AUTH_CREATED";

    private final FullResourceAccess fullResourceAccess;
    private final EventAdmin eventAdmin;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                           access to resources
     * @param eventAdmin         {@link EventAdmin} that will be used by the constructed object to post {@link Event}s
     */
    @Activate
    public AuthCreationBroadcast(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        EventAdmin eventAdmin
    ) {
        this.fullResourceAccess = fullResourceAccess;
        this.eventAdmin = eventAdmin;
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        int numOfChanges = changes.size();
        log.trace("Received {} Resource Change(s) to broadcast", numOfChanges);
        toAuthIDs(changes).stream()
            .map(authID -> Map.of(AuthID.class.getSimpleName(), authID))
            .map(props -> new Event(TOPIC_AUTH_CREATION, props))
            .forEach(
                event -> {
                    log.debug("Broadcasting {}", event);
                    eventAdmin.postEvent(event);
                }
            );
    }

    private List<AuthID> toAuthIDs(Collection<ResourceChange> changes) {
        log.trace("Converting to auth IDs: {}", changes);
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            return toAuths(changes, resourceResolver).stream()
                .map(SneakyFunction.sneaky(Authorizable::getID))
                .map(AuthIDUniversal::new)
                .map(AuthID.class::cast)
                .toList();
        }
    }

    private Collection<Authorizable> toAuths(
        Collection<ResourceChange> changes, ResourceResolver resourceResolver
    ) {
        return changes.stream()
            .map(change -> toAuth(change, resourceResolver))
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<Authorizable> toAuth(ResourceChange resourceChange, ResourceResolver resourceResolver) {
        String resourceChangePath = resourceChange.getPath();
        return Optional.ofNullable(resourceResolver.getResource(resourceChangePath))
            .map(resource -> resource.adaptTo(Node.class))
            .flatMap(node -> toAuth(node, resourceResolver));
    }

    @SneakyThrows
    private Optional<Authorizable> toAuth(Node node, ResourceResolver resourceResolver) {
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath(node), resourceResolver);
        return Optional.of(nodeProperties)
            .map(NodeProperties::primaryType)
            .filter(
                primaryType -> primaryType.equals(UserConstants.NT_REP_USER)
                    || primaryType.equals(UserConstants.NT_REP_GROUP)
            ).map(SneakyFunction.sneaky(primaryType -> node.getPath()))
            .map(
                SneakyFunction.sneaky(
                    absolutePathToAuth -> {
                        UserManager userManager = new WithUserManager(resourceResolver).get();
                        return userManager.getAuthorizableByPath(absolutePathToAuth);
                    }
                )
            );
    }
}
