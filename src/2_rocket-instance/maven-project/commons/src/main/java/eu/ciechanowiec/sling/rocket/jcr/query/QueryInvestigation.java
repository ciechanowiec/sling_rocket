package eu.ciechanowiec.sling.rocket.jcr.query;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import java.util.Optional;

import static eu.ciechanowiec.sling.rocket.jcr.query.QueryInvestigationMBean.SERVICE_DESCRIPTION;

/**
 * Investigates {@link Query}-ies written in the {@link Query#JCR_SQL2} query language.
 */
@Component(
    service = QueryInvestigationMBean.class,
    immediate = true,
    property = "jmx.objectname=eu.ciechanowiec.sling.rocket.commons:type=JCR,name=QueryInvestigation"
)
@ServiceDescription(SERVICE_DESCRIPTION)
@Slf4j
@ToString
public class QueryInvestigation extends AnnotatedStandardMBean implements QueryInvestigationMBean {

    private final FullResourceAccess fullResourceAccess;
    private final QueryLogs queryLogs;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                           access to resources
     * @param queryLogs          {@link QueryLogs} that will be used by the constructed object to retrieve related logs
     */
    @Activate
    public QueryInvestigation(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        QueryLogs queryLogs
    ) {
        super(QueryInvestigationMBean.class);
        this.fullResourceAccess = fullResourceAccess;
        this.queryLogs = queryLogs;
        log.info("{} activated", this);
    }

    @SneakyThrows
    @Override
    public String explainAndMeasure(String queryToBeExplainedAndMeasured) {
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            log.debug("Running EXPLAIN MEASURE on query: '{}'", queryToBeExplainedAndMeasured);
            QueryManager queryManager = new WithQueryManager(resourceResolver).get();
            String queryString = "EXPLAIN MEASURE %s".formatted(queryToBeExplainedAndMeasured);
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            return Optional.of(query)
                .map(SneakyFunction.sneaky(Query::execute))
                .map(SneakyFunction.sneaky(QueryResult::getRows))
                .filter(rows -> rows.getSize() == NumberUtils.INTEGER_ONE)
                .map(RowIterator::nextRow)
                .map(SneakyFunction.sneaky(row -> row.getValue("plan")))
                .map(SneakyFunction.sneaky(Value::getString))
                .map(
                    plan -> {
                        log.debug("Query plan for '{}': '{}'", queryToBeExplainedAndMeasured, plan);
                        return plan;
                    }
                ).orElseGet(
                    () -> {
                        String errorMessage = "Query plan for '%s' is not available".formatted(
                            queryToBeExplainedAndMeasured
                        );
                        log.warn(errorMessage);
                        return errorMessage;
                    }
                );
        }
    }

    @Override
    public QueryInvestigationResult investigate(String queryToBeInvestigated) {
        log.debug("Investigating '{}'", queryToBeInvestigated);
        QueryPerformanceResult queryPerformanceResult = new QueryPerformance(fullResourceAccess).checkPerformance(
            queryToBeInvestigated
        );
        return new QueryInvestigationResult(
            queryToBeInvestigated,
            new QueryPlan(explainAndMeasure(queryToBeInvestigated)),
            queryLogs,
            queryPerformanceResult
        );
    }
}
