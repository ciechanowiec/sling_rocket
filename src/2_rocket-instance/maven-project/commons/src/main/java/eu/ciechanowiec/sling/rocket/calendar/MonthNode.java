package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.IllegalPrimaryTypeException;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Existing {@link Node} of type {@link MonthNode#NT_MONTH}.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class MonthNode implements WithJCRPath, Comparable<MonthNode> {

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
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param jcrPath        {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different that {@link MonthNode#NT_MONTH}
     */
    @SuppressWarnings("WeakerAccess")
    public MonthNode(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_MONTH);
    }

    /**
     * Returns a sorted {@link List} of {@link DayNode}s from this {@link MonthNode} instance.
     *
     * @return sorted {@link List} of {@link DayNode}s from this {@link MonthNode} instance
     */
    @SuppressWarnings("WeakerAccess")
    public List<DayNode> days() {
        log.trace("Listing days of {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getChildren)
                .map(UnwrappedIteration::new)
                .map(UnwrappedIteration::stream)
                .orElse(Stream.of())
                .map(Resource::getPath)
                .map(childPath -> new DayNode(new TargetJCRPath(childPath), resourceAccess))
                .sorted()
                .toList();
        }
    }

    private Year year() {
        log.trace("Getting year of {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getParent)
                .map(Resource::getPath)
                .map(parentPath -> new YearNode(new TargetJCRPath(parentPath), resourceAccess))
                .map(YearNode::year)
                .orElseThrow(() -> new IllegalStateException("%s has no parent".formatted(this)));
        }
    }

    /**
     * Returns the calendar month represented by this {@link MonthNode}.
     *
     * @return calendar month represented by this {@link MonthNode}
     */
    @SuppressWarnings("WeakerAccess")
    public YearMonth month() {
        log.trace("Getting month of {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getValueMap)
                .map(valueMap -> valueMap.get(PN_MONTH, DefaultProperties.LONG_CLASS))
                .map(Long::intValue)
                .map(month -> year().atMonth(month))
                .orElseThrow(() -> new IllegalStateException("%s has no month property".formatted(this)));
        }
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
