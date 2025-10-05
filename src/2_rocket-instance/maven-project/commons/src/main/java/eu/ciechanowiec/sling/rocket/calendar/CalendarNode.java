package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
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
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.inject.Inject;
import javax.jcr.Node;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Existing {@link Node} of type {@link CalendarNode#NT_CALENDAR}.
 */
@Slf4j
@ToString
@EqualsAndHashCode
@Model(
    adaptables = {Resource.class, SlingJakartaHttpServletRequest.class}
)
public final class CalendarNode implements WithJCRPath {

    /**
     * The type name of a {@link Node} that represents a calendar with a specific scope of years.
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static final String NT_CALENDAR = "rocket:Calendar";

    private final JCRPath jcrPath;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Supplier<List<YearNode>> yearsSupplier;

    /**
     * Constructs an instance of this class.
     *
     * @param resource {@link Resource} backed by the {@link Node} represented by the constructed object
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different than {@link CalendarNode#NT_CALENDAR}
     */
    @Inject
    @SuppressWarnings("PMD.CloseResource")
    public CalendarNode(
        @SlingObject
        Resource resource
    ) {
        this.jcrPath = new TargetJCRPath(resource);
        assertPrimaryType(resource);
        ResourceResolver resourceResolver = resource.getResourceResolver();
        yearsSupplier = () -> {
            log.trace("Listing years of {}", this);
            return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                .map(Resource::getChildren)
                .map(Iterable::iterator)
                .map(IteratorUtils::toList)
                .stream()
                .flatMap(Collection::stream)
                .map(Resource::getPath)
                .map(childPath -> new YearNode(new TargetJCRPath(childPath), resourceResolver))
                .sorted()
                .toList();
        };
    }

    /**
     * Constructs an instance of this class.
     *
     * @param jcrPath        {@link JCRPath} pointing to the {@link Node} represented by the constructed object
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     * @throws IllegalPrimaryTypeException if the primary type of the {@link Node} represented by the constructed object
     *                                     is different than {@link CalendarNode#NT_CALENDAR}
     */
    @SuppressWarnings("WeakerAccess")
    public CalendarNode(JCRPath jcrPath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrPath;
        assertPrimaryType(resourceAccess);
        yearsSupplier = () -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                log.trace("Listing years of {}", this);
                return Optional.ofNullable(resourceResolver.getResource(jcrPath().get()))
                    .map(Resource::getChildren)
                    .map(Iterable::iterator)
                    .map(IteratorUtils::toList)
                    .stream()
                    .flatMap(Collection::stream)
                    .map(Resource::getPath)
                    .map(childPath -> new YearNode(new TargetJCRPath(childPath), resourceAccess))
                    .sorted()
                    .toList();
            }
        };
    }

    private void assertPrimaryType(ResourceAccess resourceAccess) {
        log.trace("Asserting primary type of {}", this);
        NodeProperties nodeProperties = new NodeProperties(this, resourceAccess);
        nodeProperties.assertPrimaryType(NT_CALENDAR);
    }

    private void assertPrimaryType(Resource resource) {
        log.trace("Asserting primary type of {}", this);
        ValueMap valueMap = resource.getValueMap();
        String actualPrimaryType = Objects.requireNonNull(valueMap.get(JcrConstants.JCR_PRIMARYTYPE, String.class));
        Conditional.isTrueOrThrow(
            actualPrimaryType.equals(NT_CALENDAR), new IllegalPrimaryTypeException(NT_CALENDAR)
        );
    }

    /**
     * Returns a sorted {@link List} of {@link YearNode}s from this {@link CalendarNode} instance.
     *
     * @return sorted {@link List} of {@link YearNode}s from this {@link CalendarNode} instance
     */
    @SuppressWarnings("WeakerAccess")
    public List<YearNode> years() {
        return yearsSupplier.get();
    }

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }
}
