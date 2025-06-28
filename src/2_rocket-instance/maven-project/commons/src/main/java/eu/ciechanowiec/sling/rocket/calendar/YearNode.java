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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Existing {@link Node} of type {@link YearNode#NT_YEAR}.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class YearNode implements WithJCRPath, Comparable<YearNode> {

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
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param jcrPath        {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different that {@link YearNode#NT_YEAR}
     */
    @SuppressWarnings("WeakerAccess")
    public YearNode(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_YEAR);
    }

    /**
     * Returns a sorted {@link List} of {@link MonthNode}s from this {@link YearNode} instance.
     *
     * @return sorted {@link List} of {@link MonthNode}s from this {@link YearNode} instance
     */
    @SuppressWarnings("WeakerAccess")
    public List<MonthNode> months() {
        log.trace("Listing months of {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getChildren)
                .map(UnwrappedIteration::new)
                .map(UnwrappedIteration::stream)
                .orElse(Stream.of())
                .map(Resource::getPath)
                .map(childPath -> new MonthNode(new TargetJCRPath(childPath), resourceAccess))
                .sorted()
                .toList();
        }
    }

    /**
     * Returns the calendar year represented by this {@link YearNode}.
     *
     * @return calendar year represented by this {@link YearNode}
     */
    @SuppressWarnings("WeakerAccess")
    public Year year() {
        log.trace("Getting year of {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getValueMap)
                .map(valueMap -> valueMap.get(PN_YEAR, DefaultProperties.LONG_CLASS))
                .map(Long::intValue)
                .map(Year::of)
                .orElseThrow(() -> new IllegalStateException("%s has no year property".formatted(this)));
        }
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
