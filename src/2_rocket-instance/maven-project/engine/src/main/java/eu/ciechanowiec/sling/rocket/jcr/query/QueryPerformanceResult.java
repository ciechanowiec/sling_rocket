package eu.ciechanowiec.sling.rocket.jcr.query;

import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("ClassWithTooManyFields")
class QueryPerformanceResult {

    private final String query;
    private final int pageSize;
    private final List<JCRPath> firstPageResults;
    private final long totalNumberOfResults;
    private final long queryExecutionTimeMS;
    private final long getNodesTimeMS;
    private final long readNodesInFirstPageTimeMS;
    private final long readAllNodesTimeMS;
    private final long totalTimeMS;

    @SuppressWarnings(
        {"squid:S107", "ConstructorWithTooManyParameters", "ParameterNumber", "PMD.ExcessiveParameterList"}
    )
    QueryPerformanceResult(
        String query, int pageSize, List<JCRPath> firstPageResults, long totalNumberOfResults,
        long queryExecutionTimeMS,
        long getNodesTimeMS, long readNodesInFirstPageTimeMS, long readAllNodesTimeMS, long totalTimeMS
    ) {
        this.query = query;
        this.pageSize = pageSize;
        this.firstPageResults = Collections.unmodifiableList(firstPageResults);
        this.totalNumberOfResults = totalNumberOfResults;
        this.queryExecutionTimeMS = queryExecutionTimeMS;
        this.getNodesTimeMS = getNodesTimeMS;
        this.readNodesInFirstPageTimeMS = readNodesInFirstPageTimeMS;
        this.readAllNodesTimeMS = readAllNodesTimeMS;
        this.totalTimeMS = totalTimeMS;
    }

    @Override
    public String toString() {
        return """
            QUERY PERFORMANCE RESULT:
              QUERY: %s
              PAGE SIZE: %d
              FIRST PAGE RESULTS:
            %s
              1. TOTAL NUMBER OF RESULTS:       %d
              2. QUERY EXECUTION TIME:          %d ms
                 [Query#execute()]
              3. GET NODES TIME:                %d ms
                 [QueryResult#getNodes()]
              4. READ NODES IN FIRST PAGE TIME: %d ms
                 [NodeIterator#next() * %d]
              5. READ ALL NODES TIME:           %d ms
                 [NodeIterator#next() * %d]
              6. TOTAL TIME:                    %d ms
            """.formatted(
            query,
            pageSize,
            firstPageResults(),
            totalNumberOfResults,
            queryExecutionTimeMS,
            getNodesTimeMS,
            readNodesInFirstPageTimeMS,
            pageSize,
            readAllNodesTimeMS,
            totalNumberOfResults,
            totalTimeMS
        );
    }

    private String firstPageResults() {
        String firstPageResultsFormatted = this.firstPageResults.stream()
            .map(JCRPath::get)
            .map(line -> "  - " + line)
            .collect(Collectors.joining("\n"));
        return Optional.of(firstPageResultsFormatted)
            .filter(results -> !results.isBlank())
            .orElse("  - [none]");
    }
}
