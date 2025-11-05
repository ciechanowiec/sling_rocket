package eu.ciechanowiec.sling.rocket.jcr.query;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.SneakyThrows;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

class QueryPerformance {

    private final FullResourceAccess fullResourceAccess;

    QueryPerformance(FullResourceAccess fullResourceAccess) {
        this.fullResourceAccess = fullResourceAccess;
    }

    @SuppressWarnings({"MagicNumber", "MethodWithMultipleLoops", "PMD.LongVariable"})
    @SneakyThrows
    QueryPerformanceResult checkPerformance(String queryWhosePerformanceShouldBeChecked) {
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            QueryManager queryManager = new WithQueryManager(resourceResolver).get();
            Query query = queryManager.createQuery(queryWhosePerformanceShouldBeChecked, Query.JCR_SQL2);
            long beforeQueryExecution = System.currentTimeMillis();
            QueryResult queryResult = query.execute();
            long queryExecutionTimeMS = System.currentTimeMillis() - beforeQueryExecution;
            long beforeGetNodes = System.currentTimeMillis();
            NodeIterator nodes = queryResult.getNodes();
            long getNodesTimeMS = System.currentTimeMillis() - beforeGetNodes;
            List<JCRPath> firstPageResults = new ArrayList<>();
            long totalNumberOfResults = 0;
            int pageSize = 20;
            long beforeReadingNodes = System.currentTimeMillis();
            for (; nodes.hasNext() && totalNumberOfResults < pageSize; ++totalNumberOfResults) {
                Node nextNode = nodes.nextNode();
                String nodePath = nextNode.getPath();
                firstPageResults.add(new TargetJCRPath(nodePath));
            }
            long readNodesInFirstPageTimeMS = System.currentTimeMillis() - beforeReadingNodes;
            while (nodes.hasNext()) {
                nodes.next();
                ++totalNumberOfResults;
            }
            long readAllNodesTimeMS = System.currentTimeMillis() - beforeReadingNodes;
            long totalTimeMS = queryExecutionTimeMS + getNodesTimeMS + readAllNodesTimeMS;
            return new QueryPerformanceResult(
                queryWhosePerformanceShouldBeChecked,
                pageSize,
                firstPageResults,
                totalNumberOfResults,
                queryExecutionTimeMS,
                getNodesTimeMS,
                readNodesInFirstPageTimeMS,
                readAllNodesTimeMS,
                totalTimeMS
            );
        }
    }
}
