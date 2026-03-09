package eu.ciechanowiec.sling.rocket.observation.audit.pushers;

import eu.ciechanowiec.sling.rocket.observation.audit.Entry;
import eu.ciechanowiec.sling.rocket.observation.audit.EntryTrampoline;
import eu.ciechanowiec.sling.rocket.observation.audit.Storage;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.util.List;
import java.util.function.Predicate;

/**
 * Listens to {@link ResourceChange}-s and submits corresponding {@link Entry}-s to the {@link EntryTrampoline}.
 */
@SuppressWarnings("ClassCanBeRecord")
@Component(
    service = {GenericRCL.class, ResourceChangeListener.class},
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = GenericRCLConfig.class)
@Slf4j
@ServiceDescription("Listens to resource changes and submits corresponding entries to the EntryTrampoline")
public class GenericRCL implements ResourceChangeListener {

    private final EntryTrampoline entryTrampoline;
    private final Predicate<ResourceChange> isLegalPath;

    /**
     * Constructs an instance of this class.
     *
     * @param entryTrampoline {@link EntryTrampoline} to which the constructed object will submit {@link Entry}-s
     *                        corresponding to received {@link ResourceChange}-s
     * @param storage         {@link Storage} used by the constructed instance to determine whether a received
     *                        {@link ResourceChange} is legal for processing by this class based on the path of the
     *                        {@link ResourceChange}
     */
    @Activate
    public GenericRCL(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        EntryTrampoline entryTrampoline,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        Storage storage
    ) {
        this.entryTrampoline = entryTrampoline;
        this.isLegalPath = resourceChange -> {
            String resourceChangePath = resourceChange.getPath();
            String storagePath = storage.jcrPath().get();
            return !resourceChangePath.startsWith(storagePath);
        };
    }

    @Override
    @SuppressWarnings("squid:S3864")
    public void onChange(List<ResourceChange> changes) {
        changes.stream()
            .peek(
                resourceChange -> log.trace("Received {}", resourceChange)
            ).filter(isLegalPath)
            .forEach(this::onChange);
    }

    private void onChange(ResourceChange resourceChange) {
        entryTrampoline.submitForSaving(new Entry(resourceChange));
    }
}
