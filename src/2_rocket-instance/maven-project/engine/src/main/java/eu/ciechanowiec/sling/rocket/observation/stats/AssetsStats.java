package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

/**
 * Statistics on the {@link Asset}s stored in the system.
 */
@Component(
    service = {RocketStats.class, AssetsStats.class},
    immediate = true
)
@Slf4j
@ServiceDescription("Statistics on the Assets stored in the system")
public class AssetsStats implements RocketStats {

    private final AssetsRepository assetsRepository;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                           access to resources
     */
    @Activate
    public AssetsStats(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess
    ) {
        this.assetsRepository = new AssetsRepository(fullResourceAccess);
    }

    @JsonValue
    AssetsCalculation calculate() {
        log.info("Calculating assets stats");
        return new AssetsCalculation(assetsRepository);
    }

    @Override
    public String name() {
        return AssetsStats.class.getName();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
