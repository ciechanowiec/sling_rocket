package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.IllegalPrimaryTypeException;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Optional;

/**
 * Existing {@link Node} of type {@link DayNode#NT_DAY}.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class DayNode implements WithJCRPath, Comparable<DayNode> {

    /**
     * The type name of a {@link Node} that represents a calendar day.
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static final String NT_DAY = "rocket:Day";

    /**
     * Name of a {@link Property} of type {@link PropertyType#DATE} on a {@link Node} of type {@link DayNode#NT_DAY}.
     * The {@link Property} defines the exact calendar day represented by the {@link Node}.
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static final String PN_DAY = "rocket:day";

    private final JCRPath jcrPath;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param jcrPath        {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different that {@link DayNode#NT_DAY}
     */
    @SuppressWarnings("WeakerAccess")
    public DayNode(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_DAY);
    }

    /**
     * Returns the calendar day represented by this {@link DayNode}.
     *
     * @return calendar day represented by this {@link DayNode}
     */
    @SuppressWarnings("WeakerAccess")
    public LocalDate day() {
        log.trace("Getting day of {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getValueMap)
                .map(valueMap -> valueMap.get(PN_DAY, DefaultProperties.DATE_CLASS))
                .map(this::toLocalDate)
                .orElseThrow(() -> new IllegalStateException("%s has no day property".formatted(this)));
        }
    }

    private LocalDate toLocalDate(Calendar calendar) {
        Instant instant = calendar.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        return zonedDateTime.toLocalDate();
    }

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }

    @Override
    @SuppressWarnings("squid:S1210")
    public int compareTo(DayNode other) {
        return day().compareTo(other.day());
    }
}
