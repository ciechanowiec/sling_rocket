package eu.ciechanowiec.sling.rocket.identity.mapping;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.*;
import eu.ciechanowiec.sling.rocket.identity.creation.AuthCreated;
import eu.ciechanowiec.sling.rocket.identity.creation.AuthCreationBroadcast;
import eu.ciechanowiec.sling.rocket.job.SchedulableJobConsumer;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ensures that predefined {@link Group}s contain only predefined {@link Authorizable}s as their members.
 */
@Component(
    service = {
        EventHandler.class, GroupToAuthMapping.class, GroupToAuthMappingMBean.class, JobConsumer.class,
        SchedulableJobConsumer.class
    },
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + GroupToAuthMapping.JOB_TOPIC,
        EventConstants.EVENT_TOPIC + "=" + AuthCreationBroadcast.TOPIC_AUTH_CREATION,
        "jmx.objectname=eu.ciechanowiec.sling.rocket.engine:type=Identity Management,name=Group to Auth Mapping"
    }
)
@Designate(
    ocd = GroupToAuthMappingConfig.class
)
@Slf4j
@ServiceDescription(GroupToAuthMapping.SERVICE_DESCRIPTION)
@ToString
public class GroupToAuthMapping extends AnnotatedStandardMBean implements EventHandler, GroupToAuthMappingMBean,
                                                                          SchedulableJobConsumer {

    static final String SERVICE_DESCRIPTION
        = "Ensures that predefined groups contain only predefined authorizables as their members";
    static final String JOB_TOPIC
        = "eu/ciechanowiec/sling/rocket/identity/mapping/GROU_TO_AUTH_MAPPING";

    @ToString.Exclude
    private final FullResourceAccess fullResourceAccess;
    private final AtomicReference<Map<AuthIDGroup, Set<AuthID>>> mappings;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                           access to resources
     * @param config             {@link GroupToAuthMappingConfig} used by the constructed instance
     */
    @Activate
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public GroupToAuthMapping(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess,
        GroupToAuthMappingConfig config
    ) {
        super(GroupToAuthMappingMBean.class);
        this.fullResourceAccess = fullResourceAccess;
        this.mappings = new AtomicReference<>(Map.of());
        configure(config);
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(GroupToAuthMappingConfig config) {
        String[] rawMappings = config.groups$_$to$_$auths_mappings();
        log.debug("Configuration of {} started with {}", this, Arrays.toString(rawMappings));
        mappings.set(
            Stream.of(rawMappings)
                .map(this::splitRawMapping)
                .flatMap(Optional::stream)
                .map(
                    entry -> new AbstractMap.SimpleEntry<>(
                        new AuthIDGroup(entry.getKey()),
                        splitAuthIDs(entry.getValue())
                    )
                ).filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        log.debug("Configured {}", this);
    }

    private Optional<Map.Entry<String, String>> splitRawMapping(String rawMapping) {
        String delimiter = "###";
        String[] rawMappingSplit = rawMapping.split(delimiter);
        boolean thereAreTwoTokens = rawMappingSplit.length == NumberUtils.INTEGER_TWO;
        if (thereAreTwoTokens) {
            Map.Entry<String, String> normalizedMapping = new AbstractMap.SimpleEntry<>(
                rawMappingSplit[NumberUtils.INTEGER_ZERO], rawMappingSplit[NumberUtils.INTEGER_ONE]
            );
            log.debug("'{}' was created out of '{}' raw mapping", normalizedMapping, rawMapping);
            return Optional.of(normalizedMapping);
        } else {
            log.warn("Invalid mapping: '{}'", rawMapping);
            return Optional.empty();
        }
    }

    private Set<AuthID> splitAuthIDs(String rawAuthIDs) {
        log.trace("Splitting raw auth IDs: '{}'", rawAuthIDs);
        String delimiter = "<<<>>>";
        return Stream.of(rawAuthIDs.split(delimiter))
            .filter(rawAuthID -> !rawAuthID.isBlank())
            .map(AuthIDUniversal::new)
            .collect(Collectors.toSet());
    }

    @Override
    public Map<AuthID, Set<AuthIDGroup>> mapAll() {
        return new AuthRepository(fullResourceAccess).all()
            .stream()
            .map(this::map)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map.Entry<AuthID, Set<AuthIDGroup>> map(AuthID authID) {
        log.debug("Mapping {}", authID);
        SimpleAuthorizable simpleAuthorizable = new SimpleAuthorizable(authID, fullResourceAccess);
        removeFromRedundantGroups(simpleAuthorizable);
        addToRequiredGroups(simpleAuthorizable);
        Map.Entry<AuthID, Set<AuthIDGroup>> groups = Map.entry(authID, simpleAuthorizable.groups(false));
        log.debug("Mapped {}", groups);
        return groups;
    }

    private void addToRequiredGroups(SimpleAuthorizable simpleAuthorizable) {
        log.trace("Adding {} to required groups", simpleAuthorizable);
        Map<AuthIDGroup, Set<AuthID>> mappingsUnwrapped = mappings.get();
        mappingsUnwrapped.entrySet().stream()
            .filter(mapping -> mapping.getValue().contains(simpleAuthorizable.authID()))
            .forEach(
                entry -> {
                    AuthIDGroup requiredGroup = entry.getKey();
                    log.trace("Adding {} to {}", simpleAuthorizable, requiredGroup);
                    boolean wasAdded = simpleAuthorizable.addToGroup(requiredGroup);
                    log.trace("Was {} added to {}? Answer: {}", simpleAuthorizable, requiredGroup, wasAdded);
                }
            );
    }

    @SuppressWarnings("PMD.LongVariable")
    private void removeFromRedundantGroups(SimpleAuthorizable simpleAuthorizable) {
        log.trace("Removing {} from redundant groups", simpleAuthorizable);
        Collection<AuthIDGroup> groupsOfAuth = simpleAuthorizable.groups(false);
        Map<AuthIDGroup, Set<AuthID>> mappingsUnwrapped = mappings.get();
        groupsOfAuth.stream()
            .filter(mappingsUnwrapped::containsKey)
            .filter(
                controlledGroupOfAuth -> {
                    AuthID authID = simpleAuthorizable.authID();
                    boolean isProhibitedControlledGroupOfAuth = !mappingsUnwrapped.get(controlledGroupOfAuth)
                        .contains(authID);
                    log.trace(
                        "Is {} a prohibited controlled group of {}? Answer: {}",
                        controlledGroupOfAuth, simpleAuthorizable, isProhibitedControlledGroupOfAuth
                    );
                    return isProhibitedControlledGroupOfAuth;
                }
            ).forEach(simpleAuthorizable::removeFromGroup);
    }

    /**
     * Triggers the {@link #map(AuthID)} command on the {@link Event} adaptable to the {@link AuthCreated} adapter.
     *
     * @param event {@link Event} adaptable to the {@link AuthCreated} adapter
     */
    @Override
    public void handleEvent(Event event) {
        log.trace("Handling {}", event);
        new AuthCreated(event).authID().ifPresent(this::map);
    }

    @Override
    public Map<AuthIDGroup, Set<AuthID>> getMappings() {
        return Collections.unmodifiableMap(mappings.get());
    }

    /**
     * Triggers the scheduled execution of {@link #mapAll()}.
     *
     * @param job any {@link Job}
     * @return {@link JobResult#OK} regardless of the {@link #mapAll()} outcome
     */
    @Override
    public JobResult process(Job job) {
        log.debug("Will trigger scheduled execution of {}", this);
        mapAll();
        return JobResult.OK;
    }
}
