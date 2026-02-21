package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.IllegalPrimaryTypeException;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
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
import javax.jcr.Repository;
import java.time.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
     * <p>
     * There are no restrictions towards the name of the {@link Node}.
     */
    @SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "WeakerAccess"})
    public static final String NT_CALENDAR = "rocket:Calendar";

    private final JCRPath jcrPath;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Supplier<List<YearNode>> yearsSupplier;
    private final Function<Year, Optional<YearNode>> yearFunction;
    private final Function<YearMonth, Optional<MonthNode>> monthFunction;
    private final Function<LocalDate, Optional<DayNode>> dayFunction;

    /**
     * Constructs an instance of this class utilizing an externally provided, pre-existing {@link ResourceResolver} from
     * the passed {@link Resource}.
     * <p>
     * This constructor is designed for scenarios where the lifecycle of the {@link ResourceResolver} is managed by the
     * calling context (e.g., Sling). The provided {@link Resource} is used to get a {@link ResourceResolver} for all
     * subsequent {@link Repository} operations within this object. Consequently, this object will <strong>not</strong>
     * assume ownership of the {@link ResourceResolver} and will not attempt to close it.
     * <p>
     * The object constructed with this constructor offers superior performance compared to
     * {@link CalendarNode#CalendarNode(JCRPath, ResourceAccess)} as it avoids the need to repeatedly acquire and
     * authenticate a new {@link ResourceResolver} for each {@link Repository} operation. It is therefore the preferred
     * choice in performance-sensitive code sections where a {@link ResourceResolver} is already available.
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
                .map(IteratorUtils::stream)
                .orElse(Stream.of())
                .filter(
                    child -> {
                        ValueMap valueMap = child.getValueMap();
                        return Optional.ofNullable(valueMap.get(JcrConstants.JCR_PRIMARYTYPE, String.class))
                            .filter(childPrimaryType -> childPrimaryType.equals(YearNode.NT_YEAR))
                            .isPresent();
                    }
                ).map(Resource::getPath)
                .map(childPath -> new YearNode(new TargetJCRPath(childPath), resourceResolver))
                .sorted()
                .toList();
        };
        yearFunction = year -> year(year, resourceResolver);
        monthFunction = yearMonth -> month(yearMonth, resourceResolver);
        dayFunction = day -> day(day, resourceResolver);
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
     * sections where a {@link ResourceResolver} is already available, use {@link CalendarNode#CalendarNode(Resource)}.
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
                    .map(IteratorUtils::stream)
                    .orElse(Stream.of())
                    .filter(
                        child -> {
                            ValueMap valueMap = child.getValueMap();
                            return Optional.ofNullable(valueMap.get(JcrConstants.JCR_PRIMARYTYPE, String.class))
                                .filter(childPrimaryType -> childPrimaryType.equals(YearNode.NT_YEAR))
                                .isPresent();
                        }
                    ).map(Resource::getPath)
                    .map(childPath -> new YearNode(new TargetJCRPath(childPath), resourceAccess))
                    .sorted()
                    .toList();
            }
        };
        yearFunction = year -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                return year(year, resourceResolver);
            }
        };
        monthFunction = yearMonth -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                return month(yearMonth, resourceResolver);
            }
        };
        dayFunction = day -> {
            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                return day(day, resourceResolver);
            }
        };
    }

    private Optional<YearNode> year(Year year, ResourceResolver resourceResolver) {
        JCRPath yearNodeJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath()), year.toString());
        return Optional.ofNullable(resourceResolver.getResource(yearNodeJCRPath.get()))
            .map(
                yearResource -> {
                    log.trace("Mapping {} to a Year Node", yearResource);
                    return new YearNode(yearNodeJCRPath, resourceResolver);
                }
            );
    }

    /**
     * Returns an {@link Optional} containing a {@link YearNode} from this {@link CalendarNode} for the specified
     * {@link Year}.
     *
     * @param year {@link Year} for which the {@link YearNode} is requested
     * @return {@link Optional} containing a {@link YearNode} from this {@link CalendarNode} for the specified
     * {@link Year}; empty {@link Optional} is returned if for the specified {@link Year} no {@link YearNode} exists
     */
    public Optional<YearNode> year(Year year) {
        return yearFunction.apply(year);
    }

    private Optional<MonthNode> month(YearMonth yearMonth, ResourceResolver resourceResolver) {
        String year = String.valueOf(yearMonth.getYear());
        JCRPath yearNodeJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath()), year);
        JCRPath monthNodeJCRPath = new TargetJCRPath(new ParentJCRPath(yearNodeJCRPath), yearMonth.toString());
        return Optional.ofNullable(resourceResolver.getResource(monthNodeJCRPath.get()))
            .map(
                yearResource -> {
                    log.trace("Mapping {} to a Month Node", yearResource);
                    return new MonthNode(monthNodeJCRPath, resourceResolver);
                }
            );
    }

    /**
     * Returns an {@link Optional} containing a {@link MonthNode} from this {@link CalendarNode} for the specified
     * {@link YearMonth}.
     *
     * @param yearMonth {@link YearMonth} for which the {@link MonthNode} is requested
     * @return {@link Optional} containing a {@link MonthNode} from this {@link CalendarNode} for the specified
     * {@link YearMonth}; empty {@link Optional} is returned if for the specified {@link YearMonth} no {@link MonthNode}
     * exists§
     */
    @SuppressWarnings("WeakerAccess")
    public Optional<MonthNode> month(YearMonth yearMonth) {
        return monthFunction.apply(yearMonth);
    }

    private Optional<DayNode> day(LocalDate day, ResourceResolver resourceResolver) {
        int year = day.getYear();
        Month month = day.getMonth();
        YearMonth yearMonth = YearMonth.of(year, month);
        JCRPath yearNodeJCRPath = new TargetJCRPath(new ParentJCRPath(jcrPath()), String.valueOf(year));
        JCRPath monthNodeJCRPath = new TargetJCRPath(new ParentJCRPath(yearNodeJCRPath), yearMonth.toString());
        JCRPath dayNodeJCRPath = new TargetJCRPath(new ParentJCRPath(monthNodeJCRPath), day.toString());
        return Optional.ofNullable(resourceResolver.getResource(dayNodeJCRPath.get()))
            .map(
                yearResource -> {
                    log.trace("Mapping {} to a Day Node", yearResource);
                    return new DayNode(dayNodeJCRPath, resourceResolver);
                }
            );
    }

    /**
     * Returns an {@link Optional} containing a {@link DayNode} from this {@link CalendarNode} for the specified
     * {@link LocalDate}.
     *
     * @param day {@link LocalDate} for which the {@link DayNode} is requested
     * @return {@link Optional} containing a {@link DayNode} from this {@link CalendarNode} for the specified
     * {@link LocalDate}; empty {@link Optional} is returned if for the specified {@link LocalDate} no {@link DayNode}
     * exists
     */
    @SuppressWarnings("WeakerAccess")
    public Optional<DayNode> day(LocalDate day) {
        return dayFunction.apply(day);
    }

    /**
     * Returns an {@link Optional} containing a {@link DayNode} from this {@link CalendarNode} for the specified
     * {@link LocalDateTime}.
     *
     * @param day {@link LocalDateTime} for which the {@link DayNode} is requested
     * @return {@link Optional} containing a {@link DayNode} from this {@link CalendarNode} for the specified
     * {@link LocalDateTime}; empty {@link Optional} is returned if for the specified {@link LocalDate} no
     * {@link DayNode} exists
     */
    @SuppressWarnings("WeakerAccess")
    public Optional<DayNode> day(LocalDateTime day) {
        return day(day.toLocalDate());
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
