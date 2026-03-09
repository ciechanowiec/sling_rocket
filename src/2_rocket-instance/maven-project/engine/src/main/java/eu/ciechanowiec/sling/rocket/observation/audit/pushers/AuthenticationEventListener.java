package eu.ciechanowiec.sling.rocket.observation.audit.pushers;

import eu.ciechanowiec.sling.rocket.identity.AuthID;
import eu.ciechanowiec.sling.rocket.identity.creation.AuthCreated;
import eu.ciechanowiec.sling.rocket.identity.creation.AuthCreationBroadcast;
import eu.ciechanowiec.sling.rocket.observation.audit.Entry;
import eu.ciechanowiec.sling.rocket.observation.audit.EntryTrampoline;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Listens to relevant authentication events and submits corresponding {@link Entry}-s to the {@link EntryTrampoline}.
 */
@Component(
    service = {AuthenticationEventListener.class, EventHandler.class},
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        EventConstants.EVENT_TOPIC + "=" + AuthCreationBroadcast.TOPIC_AUTH_CREATION,
        EventConstants.EVENT_TOPIC + "=" + AuthConstants.TOPIC_LOGIN,
        EventConstants.EVENT_TOPIC + "=" + AuthConstants.TOPIC_LOGIN_FAILED
    }
)
@Designate(ocd = AuthenticationEventListenerConfig.class)
@Slf4j
@ToString
@ServiceDescription(
    "Listens to relevant authentication events and "
        + "submits corresponding entries to the EntryTrampoline"
)
public class AuthenticationEventListener implements EventHandler {

    private final AtomicReference<AuthenticationEventListenerConfig> config;
    @ToString.Exclude
    private final EntryTrampoline entryTrampoline;
    @ToString.Exclude
    private final Map<String, Function<Event, Entry>> eventToEntryMappers;

    /**
     * Constructs an instance of this class.
     *
     * @param entryTrampoline {@link EntryTrampoline} to which the constructed object will submit {@link Entry}-s
     *                        corresponding to received events
     * @param config          {@link AuthenticationEventListenerConfig} used by the constructed instance
     */
    @Activate
    @SuppressWarnings({"squid:S1192", "MethodLength"})
    public AuthenticationEventListener(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        EntryTrampoline entryTrampoline,
        AuthenticationEventListenerConfig config
    ) {
        this.config = new AtomicReference<>(config);
        this.entryTrampoline = entryTrampoline;
        this.eventToEntryMappers = Map.of(
            AuthConstants.TOPIC_LOGIN, event -> {
                String userID = Optional.ofNullable(event.getProperty(SlingConstants.PROPERTY_USERID))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElse(Entry.UNKNOWN);
                String authType = Optional.ofNullable(event.getProperty(AuthenticationInfo.AUTH_TYPE))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElse(Entry.UNKNOWN);
                Entry entry = new Entry(
                    userID, AuthConstants.TOPIC_LOGIN, LocalDateTime.now(),
                    Map.of(AuthenticationInfo.AUTH_TYPE, authType)
                );
                log.trace("Mapped {} to {}", event, entry);
                return entry;
            },
            AuthConstants.TOPIC_LOGIN_FAILED, event -> {
                String userID = Optional.ofNullable(event.getProperty(SlingConstants.PROPERTY_USERID))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElse(Entry.UNKNOWN);
                String authType = Optional.ofNullable(event.getProperty(AuthenticationInfo.AUTH_TYPE))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElse(Entry.UNKNOWN);
                String reasonCode = Optional.ofNullable(event.getProperty("reason_code"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElse(Entry.UNKNOWN);
                Entry entry = new Entry(
                    userID, AuthConstants.TOPIC_LOGIN_FAILED, LocalDateTime.now(),
                    Map.of(AuthenticationInfo.AUTH_TYPE, authType, "reason_code", reasonCode)
                );
                log.trace("Mapped {} to {}", event, entry);
                return entry;
            },
            AuthCreationBroadcast.TOPIC_AUTH_CREATION, event -> {
                String userID = new AuthCreated(event).authID().map(AuthID::get).orElse(Entry.UNKNOWN);
                Entry entry = new Entry(
                    userID, AuthCreationBroadcast.TOPIC_AUTH_CREATION, LocalDateTime.now(), Map.of()
                );
                log.trace("Mapped {} to {}", event, entry);
                return entry;
            }
        );
        log.info("Activated {}", this);
    }

    @Modified
    void configure(AuthenticationEventListenerConfig config) {
        this.config.set(config);
        log.info("Reconfigured {}", this);
    }

    @Override
    public void handleEvent(Event event) {
        log.trace("Received {}", event);
        String topic = event.getTopic();
        Optional.ofNullable(eventToEntryMappers.get(topic))
            .map(mapper -> mapper.apply(event))
            .ifPresent(entryTrampoline::submitForSaving);
    }
}
