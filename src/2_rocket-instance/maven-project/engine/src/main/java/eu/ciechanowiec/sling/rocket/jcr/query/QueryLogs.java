package eu.ciechanowiec.sling.rocket.jcr.query;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.MDC;

import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.List;
import java.util.UUID;

/**
 * {@link Query} logs.
 */
@Slf4j
@Component(
    service = QueryLogs.class,
    immediate = true
)
@ServiceDescription("JCR query logs")
@ToString
public class QueryLogs {

    private final FullResourceAccess fullResourceAccess;
    private final QueryLogsInterception queryLogsInterception;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess    {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                              access to resources
     * @param queryLogsInterception {@link QueryLogsInterception} that will be used by the constructed object to
     *                              intercept {@link Query} logs
     */
    @Activate
    public QueryLogs(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        QueryLogsInterception queryLogsInterception
    ) {
        this.fullResourceAccess = fullResourceAccess;
        this.queryLogsInterception = queryLogsInterception;
        log.info("{} activated", this);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "squid:S899"})
    @SneakyThrows
    List<String> logsForQuery(String queryToBeLogged) {
        log.debug("Extracting logs for query: '{}'", queryToBeLogged);
        try (
            QueryLogsInterceptionSwitch queryLogsInterceptionSwitch = new QueryLogsInterceptionSwitch(
                queryLogsInterception
            );
            ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()
        ) {
            QueryManager queryManager = new WithQueryManager(resourceResolver).get();
            Query query = queryManager.createQuery(queryToBeLogged, Query.JCR_SQL2);
            query.execute().getRows().hasNext(); // Consume result to trigger all logs
            String interceptionKey = queryLogsInterceptionSwitch.interceptionKey();
            List<String> logs = queryLogsInterception.savedILoggingEvents(interceptionKey);
            log.trace("For query '{}' these logs extracted: {}", queryToBeLogged, logs);
            return logs;
        }
    }

    private static final class QueryLogsInterceptionSwitch implements AutoCloseable {

        private final QueryLogsInterception queryLogsInterception;
        private final String interceptionKey;

        QueryLogsInterceptionSwitch(QueryLogsInterception queryLogsInterception) {
            this.queryLogsInterception = queryLogsInterception;
            this.interceptionKey = UUID.randomUUID().toString();
            MDC.put(QueryLogsInterception.INTERCEPTION_KEY_NAME, interceptionKey);
        }

        String interceptionKey() {
            return interceptionKey;
        }

        @Override
        public void close() {
            MDC.remove(QueryLogsInterception.INTERCEPTION_KEY_NAME);
            queryLogsInterception.stopInterception(interceptionKey);
        }
    }
}
