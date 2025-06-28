package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Existing {@link Node} of type {@link CalendarNode#NT_CALENDAR}.
 */
@Slf4j
@ToString
@EqualsAndHashCode
public class CalendarNode implements WithJCRPath {

    /**
     * The type name of a {@link Node} that represents a calendar with a specific scope of years.
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static final String NT_CALENDAR = "rocket:Calendar";

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
     *                                     is different that {@link CalendarNode#NT_CALENDAR}
     */
    @SuppressWarnings("WeakerAccess")
    public CalendarNode(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        this.resourceAccess = resourceAccess;
        assertPrimaryType();
    }

    private void assertPrimaryType() {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_CALENDAR);
    }

    /**
     * Returns a sorted {@link List} of {@link YearNode}s from this {@link CalendarNode} instance.
     *
     * @return sorted {@link List} of {@link YearNode}s from this {@link CalendarNode} instance
     */
    @SuppressWarnings("WeakerAccess")
    public List<YearNode> years() {
        log.trace("Listing years of {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getChildren)
                .map(UnwrappedIteration::new)
                .map(UnwrappedIteration::stream)
                .orElse(Stream.of())
                .map(Resource::getPath)
                .map(childPath -> new YearNode(new TargetJCRPath(childPath), resourceAccess))
                .sorted()
                .toList();
        }
    }

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }
}
