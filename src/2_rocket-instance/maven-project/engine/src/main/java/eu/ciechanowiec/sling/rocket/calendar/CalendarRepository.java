package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Repository;
import javax.jcr.query.Query;
import java.util.List;

/**
 * Repository for {@link CalendarNode}s.
 */
@Slf4j
@ToString
public class CalendarRepository {

    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param resourceAccess {@link ResourceAccess} that will be used to acquire access to resources
     */
    @SuppressWarnings("WeakerAccess")
    public CalendarRepository(ResourceAccess resourceAccess) {
        this.resourceAccess = resourceAccess;
        log.trace("Initialized {}", this);
    }

    /**
     * Finds all {@link CalendarNode}s that are located at the specified {@link JCRPath}. All and exclusively
     * {@link CalendarNode}s that are located exactly at the specified {@link JCRPath} and its descendants are
     * returned.
     *
     * @param searchedPath {@link JCRPath} where the {@link CalendarNode}s are searched
     * @return all {@link CalendarNode}s that are located at the specified {@link JCRPath}
     */
    @SuppressWarnings("WeakerAccess")
    public List<CalendarNode> find(JCRPath searchedPath) {
        log.debug("{} searching for Calendar Nodes at {}", this, searchedPath);
        String nodeTypesQueryPart = "node.[%s] = '%s'".formatted(
            JcrConstants.JCR_PRIMARYTYPE, CalendarNode.NT_CALENDAR
        );
        String query = String.format(
            "SELECT * FROM [%s] AS node WHERE (%s) AND ISDESCENDANTNODE(node, '%s')",
            JcrConstants.NT_BASE, nodeTypesQueryPart, searchedPath.get()
        );
        log.trace("This query was built by {} to retrieve Calendar Nodes: {}", this, query);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            List<CalendarNode> calendarNodes = IteratorUtils.toList(
                    resourceResolver.findResources(query, Query.JCR_SQL2)
                ).stream()
                .filter(
                    resource -> new NodeProperties(new TargetJCRPath(resource), resourceAccess)
                        .isPrimaryType(CalendarNode.NT_CALENDAR)
                ).map(jcrPath -> new CalendarNode(new TargetJCRPath(jcrPath), resourceAccess))
                .distinct()
                .toList();
            log.debug("{} found {} Calendar Nodes with this query: {}", this, calendarNodes.size(), query);
            return calendarNodes;
        }
    }

    /**
     * Retrieves all {@link CalendarNode}s stored in the {@link Repository}.
     *
     * @return all {@link CalendarNode}s stored in the {@link Repository}
     */
    public List<CalendarNode> all() {
        log.debug("{} retrieving all Calendar Nodes", this);
        return find(new TargetJCRPath("/"));
    }
}
