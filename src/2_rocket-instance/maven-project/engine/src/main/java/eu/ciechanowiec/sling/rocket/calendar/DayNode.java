package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.IllegalPrimaryTypeException;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Existing {@link Node} of type {@link DayNode#NT_DAY}.
 */
@Slf4j
@ToString
@EqualsAndHashCode
@SuppressWarnings("PMD.OverrideBothEqualsAndHashCodeOnComparable")
public final class DayNode implements WithJCRPath, Comparable<DayNode> {

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
    private final Supplier<LocalDate> daySupplier;

    /**
     * Constructs an instance of this class utilizing an externally-provided, pre-existing {@link ResourceResolver}.
     * <p>
     * This constructor is designed for scenarios where the lifecycle of the {@link ResourceResolver} is managed by the
     * calling context. The provided {@link ResourceResolver} is used for all subsequent {@link Repository} operations
     * within this object. Consequently, this object will <strong>not</strong> assume ownership of the
     * {@link ResourceResolver} and will not attempt to close it.
     * <p>
     * The object constructed with this constructor offers superior performance compared to
     * {@link DayNode#DayNode(JCRPath, ResourceAccess)} as it avoids the need to repeatedly acquire and authenticate a
     * new {@link ResourceResolver} for each {@link Repository} operation. It is therefore the preferred choice in
     * performance-sensitive code sections where a {@link ResourceResolver} is already available.
     *
     * @param jcrPath          {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceResolver {@link ResourceResolver} that will be used by the constructed object to acquire access to
     *                         resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different than {@link DayNode#NT_DAY}
     */
    @SuppressWarnings("WeakerAccess")
    public DayNode(JCRPath jcrPath, ResourceResolver resourceResolver) {
        this.jcrPath = jcrPath;
        String jcrPathRaw = jcrPath().get();
        Resource dayNodeResource = Objects.requireNonNull(resourceResolver.getResource(jcrPathRaw));
        assertPrimaryType(dayNodeResource);
        this.daySupplier = () -> Optional.of(dayNodeResource)
            .map(Resource::getValueMap)
            .map(valueMap -> valueMap.get(PN_DAY, DefaultProperties.DATE_CLASS))
            .map(this::toLocalDate)
            .orElseThrow(() -> new IllegalStateException("%s has no day property".formatted(this)));
    }

    /**
     * Constructs an instance of this class using a {@link ResourceAccess} object to manage {@link ResourceResolver}
     * instances.
     * <p>
     * This constructor is designed for scenarios where the lifecycle of the {@link ResourceResolver} is managed by this
     * object. The provided {@link ResourceAccess} is used to acquire and subsequently close a {@link ResourceResolver}
     * for each {@link Repository} operation.
     * <p>
     * This object will assume ownership of the {@link ResourceResolver} it acquires and will close it automatically.
     * However, this might introduce a performance overhead due to repeated acquisitions. For performance-sensitive code
     * sections where a {@link ResourceResolver} is already available, use
     * {@link DayNode#DayNode(JCRPath, ResourceResolver)}.
     *
     * @param jcrPath        {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different than {@link DayNode#NT_DAY}
     */
    @SuppressWarnings("WeakerAccess")
    public DayNode(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        assertPrimaryType(resourceAccess);
        daySupplier = () -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                log.trace("Getting day of {}", this);
                return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                    .map(Resource::getValueMap)
                    .map(valueMap -> valueMap.get(PN_DAY, DefaultProperties.DATE_CLASS))
                    .map(this::toLocalDate)
                    .orElseThrow(() -> new IllegalStateException("%s has no day property".formatted(this)));
            }
        };
    }

    private void assertPrimaryType(ResourceAccess resourceAccess) {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_DAY);
    }

    private void assertPrimaryType(Resource resource) {
        log.trace("Asserting primary type of {}", this);
        ValueMap valueMap = resource.getValueMap();
        String actualPrimaryType = Objects.requireNonNull(valueMap.get(JcrConstants.JCR_PRIMARYTYPE, String.class));
        Conditional.isTrueOrThrow(
            actualPrimaryType.equals(NT_DAY), new IllegalPrimaryTypeException(NT_DAY)
        );
    }

    /**
     * Returns the calendar day represented by this {@link DayNode}.
     *
     * @return calendar day represented by this {@link DayNode}
     */
    @SuppressWarnings("WeakerAccess")
    public LocalDate day() {
        return daySupplier.get();
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
