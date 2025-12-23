package eu.ciechanowiec.sling.rocket.identity.creation;

import eu.ciechanowiec.sling.rocket.identity.AuthID;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.osgi.service.event.Event;

import javax.jcr.Repository;
import java.util.Optional;

/**
 * Adapter for the {@link Event} which is submitted to the {@link AuthCreationBroadcast#TOPIC_AUTH_CREATION} topic.
 */
public class AuthCreated {

    private final Event event;

    /**
     * Constructs an instance of this class.
     *
     * @param event {@link Event} submitted to the {@link AuthCreationBroadcast#TOPIC_AUTH_CREATION} topic
     */
    public AuthCreated(Event event) {
        this.event = event;
    }

    /**
     * Returns the {@link AuthID} of the {@link Authorizable} whose creation and persisting in the {@link Repository}
     * was captured and broadcasted through the {@link AuthCreationBroadcast#TOPIC_AUTH_CREATION} topic by the
     * {@link AuthCreationBroadcast} service.
     *
     * @return {@link AuthID} of the {@link Authorizable} whose creation and persisting in the {@link Repository} was
     * captured and broadcasted through the {@link AuthCreationBroadcast#TOPIC_AUTH_CREATION} topic by the
     * {@link AuthCreationBroadcast} service
     */
    public Optional<AuthID> authID() {
        return Optional.ofNullable(event.getProperty(AuthID.class.getSimpleName()))
            .filter(authID -> event.getTopic().equals(AuthCreationBroadcast.TOPIC_AUTH_CREATION))
            .filter(AuthID.class::isInstance)
            .map(AuthID.class::cast);
    }
}
