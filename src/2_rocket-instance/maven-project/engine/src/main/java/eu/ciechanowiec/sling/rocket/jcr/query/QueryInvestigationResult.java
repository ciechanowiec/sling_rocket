package eu.ciechanowiec.sling.rocket.jcr.query;

import eu.ciechanowiec.sling.rocket.jcr.index.IndexDescriptorWithQueryCost;

import javax.jcr.query.Query;
import java.util.stream.Collectors;

/**
 * Result of the investigation of a {@link Query} written in the {@link Query#JCR_SQL2} query language.
 */
public class QueryInvestigationResult {

    private final String query;
    private final QueryPlan queryPlan;
    private final QueryLogs queryLogs;
    private final QueryPerformanceResult queryPerformanceResult;

    QueryInvestigationResult(
        String query, QueryPlan queryPlan, QueryLogs queryLogs,
        QueryPerformanceResult queryPerformanceResult
    ) {
        this.query = query;
        this.queryPlan = queryPlan;
        this.queryLogs = queryLogs;
        this.queryPerformanceResult = queryPerformanceResult;
    }

    @Override
    public String toString() {
        String indexClassForQueryPlan = queryPlan.indexDescriptor().indexClass();
        return """
            QUERY:
            %s

            QUERY PLAN:
            %s

            INDEX CLASS IN QUERY PLAN:
            %s

            QUERY COST PER INDEX:
            %s

            %s""".formatted(
            query,
            queryPlan,
            indexClassForQueryPlan,
            queryCostPerIndex(),
            queryPerformanceResult.toString()
        );
    }

    private String queryCostPerIndex() {
        QueryCostFromQueryLogs queryCostFromQueryLogs = new QueryCostFromQueryLogs(queryLogs);
        return queryCostFromQueryLogs.queryCostPerIndex(query)
            .stream()
            .map(IndexDescriptorWithQueryCost::toString)
            .map(line -> " - " + line)
            .collect(Collectors.joining("\n"));
    }
}
