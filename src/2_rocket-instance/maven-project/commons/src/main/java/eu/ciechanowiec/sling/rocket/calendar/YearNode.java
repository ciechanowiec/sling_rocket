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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Existing {@link Node} of type {@link YearNode#NT_YEAR}.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public final class YearNode implements WithJCRPath, Comparable<YearNode> {

    /**
     * The type name of a {@link Node} that represents a calendar year.
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static final String NT_YEAR = "rocket:Year";

    /**
     * Name of a {@link Property} of type {@link PropertyType#LONG} on a {@link Node} of type {@link YearNode#NT_YEAR}.
     * The {@link Property} defines the exact calendar year represented by the {@link Node}.
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static final String PN_YEAR = "rocket:year";

    private final JCRPath jcrPath;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Supplier<Year> yearSupplier;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Supplier<List<MonthNode>> monthsSupplier;

    /**
     * Constructs an instance of this class utilizing an externally-provided, pre-existing {@link ResourceResolver}.
     * <p>
     * This constructor is designed for scenarios where the lifecycle of the {@link ResourceResolver} is managed by the
     * calling context. The provided {@link ResourceResolver} is used for all subsequent {@link Repository} operations
     * within this object. Consequently, this object will <strong>not</strong> assume ownership of the
     * {@link ResourceResolver} and will not attempt to close it.
     * <p>
     * The object constructed with this constructor offers superior performance compared to
     * {@link YearNode#YearNode(JCRPath, ResourceAccess)} as it avoids the need to repeatedly acquire and authenticate a
     * new {@link ResourceResolver} for each {@link Repository} operation. It is therefore the preferred choice in
     * performance-sensitive code sections where a {@link ResourceResolver} is already available.
     *
     * @param jcrPath          {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceResolver {@link ResourceResolver} that will be used by the constructed object to acquire access to
     *                         resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different than {@link YearNode#NT_YEAR}
     */
    @SuppressWarnings("WeakerAccess")
    public YearNode(JCRPath jcrPath, ResourceResolver resourceResolver) {
        this.jcrPath = jcrPath;
        String jcrPathRaw = jcrPath().get();
        Resource yearNodeResource = Objects.requireNonNull(resourceResolver.getResource(jcrPathRaw));
        assertPrimaryType(yearNodeResource);
        monthsSupplier = () -> {
            log.trace("Listing months of {}", this);
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getChildren)
                .map(Iterable::iterator)
                .map(IteratorUtils::toList)
                .stream()
                .flatMap(Collection::stream)
                .map(Resource::getPath)
                .map(childPath -> new MonthNode(new TargetJCRPath(childPath), resourceResolver))
                .sorted()
                .toList();
        };
        yearSupplier = () -> Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
            .map(Resource::getValueMap)
            .map(valueMap -> valueMap.get(PN_YEAR, DefaultProperties.LONG_CLASS))
            .map(Long::intValue)
            .map(Year::of)
            .orElseThrow(() -> new IllegalStateException("%s has no year property".formatted(this)));
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
     * {@link YearNode#YearNode(JCRPath, ResourceResolver)}.
     *
     * @param jcrPath        {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different than {@link YearNode#NT_YEAR}
     */
    @SuppressWarnings("WeakerAccess")
    public YearNode(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        assertPrimaryType(resourceAccess);
        monthsSupplier = () -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                log.trace("Listing months of {}", this);
                return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                    .map(Resource::getChildren)
                    .map(Iterable::iterator)
                    .map(IteratorUtils::toList)
                    .stream()
                    .flatMap(Collection::stream)
                    .map(Resource::getPath)
                    .map(childPath -> new MonthNode(new TargetJCRPath(childPath), resourceAccess))
                    .sorted()
                    .toList();
            }
        };
        yearSupplier = () -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                log.trace("Getting year of {}", this);
                return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                    .map(Resource::getValueMap)
                    .map(valueMap -> valueMap.get(PN_YEAR, DefaultProperties.LONG_CLASS))
                    .map(Long::intValue)
                    .map(Year::of)
                    .orElseThrow(() -> new IllegalStateException("%s has no year property".formatted(this)));
            }
        };
    }

    private void assertPrimaryType(ResourceAccess resourceAccess) {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_YEAR);
    }

    private void assertPrimaryType(Resource resource) {
        log.trace("Asserting primary type of {}", this);
        ValueMap valueMap = resource.getValueMap();
        String actualPrimaryType = Objects.requireNonNull(valueMap.get(JcrConstants.JCR_PRIMARYTYPE, String.class));
        Conditional.isTrueOrThrow(
            actualPrimaryType.equals(NT_YEAR), new IllegalPrimaryTypeException(NT_YEAR)
        );
    }

    /**
     * Returns a sorted {@link List} of {@link MonthNode}s from this {@link YearNode} instance.
     *
     * @return sorted {@link List} of {@link MonthNode}s from this {@link YearNode} instance
     */
    @SuppressWarnings("WeakerAccess")
    public List<MonthNode> months() {
        return monthsSupplier.get();
    }

    /**
     * Returns the calendar year represented by this {@link YearNode}.
     *
     * @return calendar year represented by this {@link YearNode}
     */
    @SuppressWarnings("WeakerAccess")
    public Year year() {
        return yearSupplier.get();
    }

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }

    @Override
    @SuppressWarnings("squid:S1210")
    public int compareTo(YearNode other) {
        return year().compareTo(other.year());
    }
}
