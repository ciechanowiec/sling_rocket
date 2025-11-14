package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.IllegalPrimaryTypeException;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import java.time.Year;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Existing {@link Node} of type {@link MonthNode#NT_MONTH}.
 */
@Slf4j
@ToString
@EqualsAndHashCode
@SuppressWarnings("PMD.OverrideBothEqualsAndHashCodeOnComparable")
public final class MonthNode implements WithJCRPath, Comparable<MonthNode> {

    /**
     * The type name of a {@link Node} that represents a calendar month.
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static final String NT_MONTH = "rocket:Month";

    /**
     * Name of a {@link Property} of type {@link PropertyType#LONG} on a {@link Node} of type
     * {@link MonthNode#NT_MONTH}. The {@link Property} defines the exact sequential number of the calendar month
     * represented by the {@link Node} within a given calendar year. The value can be anything between {@code 1} (first
     * calendar month in a given year) inclusively and {@code 12} (last calendar month in a given year) inclusively.
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static final String PN_MONTH = "rocket:month";

    private final JCRPath jcrPath;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Supplier<List<DayNode>> daysSupplier;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Supplier<YearMonth> monthSupplier;

    /**
     * Constructs an instance of this class utilizing an externally-provided, pre-existing {@link ResourceResolver}.
     * <p>
     * This constructor is designed for scenarios where the lifecycle of the {@link ResourceResolver} is managed by the
     * calling context. The provided {@link ResourceResolver} is used for all subsequent {@link Repository} operations
     * within this object. Consequently, this object will <strong>not</strong> assume ownership of the
     * {@link ResourceResolver} and will not attempt to close it.
     * <p>
     * The object constructed with this constructor offers superior performance compared to
     * {@link MonthNode#MonthNode(JCRPath, ResourceAccess)} as it avoids the need to repeatedly acquire and authenticate
     * a new {@link ResourceResolver} for each {@link Repository} operation. It is therefore the preferred choice in
     * performance-sensitive code sections where a {@link ResourceResolver} is already available.
     *
     * @param jcrPath          {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceResolver {@link ResourceResolver} that will be used by the constructed object to acquire access to
     *                         resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different than {@link MonthNode#NT_MONTH}
     */
    @SuppressWarnings("WeakerAccess")
    public MonthNode(JCRPath jcrPath, ResourceResolver resourceResolver) {
        this.jcrPath = jcrPath;
        String jcrPathRaw = jcrPath().get();
        Resource monthNodeResource = Objects.requireNonNull(resourceResolver.getResource(jcrPathRaw));
        assertPrimaryType(monthNodeResource);
        daysSupplier = () -> {
            log.trace("Listing days of {}", this);
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getChildren)
                .map(Iterable::iterator)
                .map(IteratorUtils::toList)
                .stream()
                .flatMap(Collection::stream)
                .map(Resource::getPath)
                .map(childPath -> new DayNode(new TargetJCRPath(childPath), resourceResolver))
                .sorted()
                .toList();
        };
        Supplier<Year> yearSupplier = () -> {
            log.trace("Getting year of {}", this);
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getParent)
                .map(Resource::getPath)
                .map(parentPath -> new YearNode(new TargetJCRPath(parentPath), resourceResolver))
                .map(YearNode::year)
                .orElseThrow(() -> new IllegalStateException("%s has no parent".formatted(this)));
        };
        monthSupplier = () -> {
            log.trace("Getting month of {}", this);
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getValueMap)
                .map(valueMap -> valueMap.get(PN_MONTH, DefaultProperties.LONG_CLASS))
                .map(Long::intValue)
                .map(month -> yearSupplier.get().atMonth(month))
                .orElseThrow(() -> new IllegalStateException("%s has no month property".formatted(this)));
        };
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
     * {@link MonthNode#MonthNode(JCRPath, ResourceResolver)}.
     *
     * @param jcrPath        {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different than {@link MonthNode#NT_MONTH}
     */
    @SuppressWarnings("WeakerAccess")
    public MonthNode(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        assertPrimaryType(resourceAccess);
        daysSupplier = () -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                log.trace("Listing days of {}", this);
                return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                    .map(Resource::getChildren)
                    .map(Iterable::iterator)
                    .map(IteratorUtils::toList)
                    .stream()
                    .flatMap(Collection::stream)
                    .map(Resource::getPath)
                    .map(childPath -> new DayNode(new TargetJCRPath(childPath), resourceAccess))
                    .sorted()
                    .toList();
            }
        };
        Supplier<Year> yearSupplier = () -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                log.trace("Getting year of {}", this);
                return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                    .map(Resource::getParent)
                    .map(Resource::getPath)
                    .map(parentPath -> new YearNode(new TargetJCRPath(parentPath), resourceAccess))
                    .map(YearNode::year)
                    .orElseThrow(() -> new IllegalStateException("%s has no parent".formatted(this)));
            }
        };
        monthSupplier = () -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                log.trace("Getting month of {}", this);
                return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                    .map(Resource::getValueMap)
                    .map(valueMap -> valueMap.get(PN_MONTH, DefaultProperties.LONG_CLASS))
                    .map(Long::intValue)
                    .map(month -> yearSupplier.get().atMonth(month))
                    .orElseThrow(() -> new IllegalStateException("%s has no month property".formatted(this)));
            }
        };
    }

    private void assertPrimaryType(ResourceAccess resourceAccess) {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_MONTH);
    }

    private void assertPrimaryType(Resource resource) {
        log.trace("Asserting primary type of {}", this);
        ValueMap valueMap = resource.getValueMap();
        String actualPrimaryType = Objects.requireNonNull(valueMap.get(JcrConstants.JCR_PRIMARYTYPE, String.class));
        Conditional.isTrueOrThrow(
            actualPrimaryType.equals(NT_MONTH), new IllegalPrimaryTypeException(NT_MONTH)
        );
    }

    /**
     * Returns a sorted {@link List} of {@link DayNode}s from this {@link MonthNode} instance.
     *
     * @return sorted {@link List} of {@link DayNode}s from this {@link MonthNode} instance
     */
    @SuppressWarnings("WeakerAccess")
    public List<DayNode> days() {
        return daysSupplier.get();
    }

    /**
     * Returns the calendar month represented by this {@link MonthNode}.
     *
     * @return calendar month represented by this {@link MonthNode}
     */
    @SuppressWarnings("WeakerAccess")
    public YearMonth month() {
        return monthSupplier.get();
    }

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }

    @Override
    @SuppressWarnings("squid:S1210")
    public int compareTo(MonthNode other) {
        return month().compareTo(other.month());
    }
}
