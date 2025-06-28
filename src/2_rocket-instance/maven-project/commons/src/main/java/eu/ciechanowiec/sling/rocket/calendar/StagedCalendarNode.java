package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.StagedNode;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import javax.jcr.Node;
import javax.jcr.Repository;
import java.time.Year;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A request to save a new {@link CalendarNode} in the {@link Repository} as a {@link Node} of type
 * {@link CalendarNode#NT_CALENDAR}.
 */
@Slf4j
public class StagedCalendarNode implements StagedNode<CalendarNode> {

    private final Set<Year> years;
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param firstYearInclusively first year (inclusive) of the calendar range
     * @param lastYearInclusively  last year (inclusive) of the calendar range; if this is the same as the
     *                             {@code firstYearInclusively}, the calendar range will consist of exactly one year
     * @param resourceAccess       {@link ResourceAccess} that will be used by the constructed object to acquire access
     *                             to resources
     */
    @SuppressWarnings("WeakerAccess")
    public StagedCalendarNode(
        Year firstYearInclusively, Year lastYearInclusively, ResourceAccess resourceAccess
    ) {
        years = IntStream.rangeClosed(firstYearInclusively.getValue(), lastYearInclusively.getValue())
            .mapToObj(Year::of)
            .collect(Collectors.toUnmodifiableSet());
        Conditional.onTrueExecute(
            lastYearInclusively.isBefore(firstYearInclusively), () -> log.warn(
                "Ending year {} is before starting year {}", lastYearInclusively, firstYearInclusively
            )
        );
        this.resourceAccess = resourceAccess;
    }

    @SneakyThrows
    @Override
    public CalendarNode save(TargetJCRPath targetJCRPath) {
        log.debug("Saving at {}", targetJCRPath);
        targetJCRPath.assertThatJCRPathIsFree(resourceAccess);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Resource calendarResource = ResourceUtil.getOrCreateResource(
                resourceResolver, targetJCRPath.get(), Map.of(JcrConstants.JCR_PRIMARYTYPE, CalendarNode.NT_CALENDAR),
                null, false
            );
            log.debug("Staged {}", calendarResource);
            years.stream()
                .map(year -> new StagedYearNode(new ParentJCRPath(targetJCRPath), year))
                .forEach(yearNode -> yearNode.stageForSaving(resourceResolver));
            resourceResolver.commit();
        }
        return new CalendarNode(targetJCRPath, resourceAccess);
    }
}
