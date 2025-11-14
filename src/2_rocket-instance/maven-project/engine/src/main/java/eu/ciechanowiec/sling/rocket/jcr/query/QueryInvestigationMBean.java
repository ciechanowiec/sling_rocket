package eu.ciechanowiec.sling.rocket.jcr.query;

import org.apache.jackrabbit.oak.api.jmx.Description;
import org.apache.jackrabbit.oak.api.jmx.Name;

import javax.jcr.query.Query;

/**
 * Investigates {@link Query}-ies written in the {@link Query#JCR_SQL2} query language.
 */
@Description(QueryInvestigationMBean.SERVICE_DESCRIPTION)
@SuppressWarnings({"LineLength", "WeakerAccess"})
public interface QueryInvestigationMBean {

    /**
     * Description of {@link QueryInvestigationMBean}.
     */
    String SERVICE_DESCRIPTION = "Investigates queries written in the JCR-SQL2 query language";

    /**
     * Run the {@code EXPLAIN MEASURE} command (see <a
     * href="https://jackrabbit.apache.org/oak/docs/query/grammar-sql2.html#explain">the documentation for details</a>)
     * on the specified {@link Query} written in the {@link Query#JCR_SQL2} query language and return the calculated
     * plan for that {@link Query}.
     *
     * @param queryToBeExplainedAndMeasured {@link Query} written in the {@link Query#JCR_SQL2} query language that
     *                                      should be explained and measured
     * @return calculated plan for the specified {@link Query}
     */
    @Description(
        "Runs the `EXPLAIN MEASURE` command on the specified query written in the "
            + "JCR-SQL2 query language and returns the calculated plan for that query"
    )
    String explainAndMeasure(
        @Name("queryToBeExplainedAndMeasured")
        @Description("Query written in the JCR-SQL2 query language that should be explained and measured")
        String queryToBeExplainedAndMeasured
    );

    /**
     * Investigate the {@link Query} written in the {@link Query#JCR_SQL2} query language.
     *
     * @param queryToBeInvestigated {@link Query} written in the {@link Query#JCR_SQL2} query language that should be
     *                              investigated
     * @return {@link QueryInvestigationResult}
     */
    @SuppressWarnings("unused")
    @Description(
        "Investigate the query written in the JCR-SQL2 query language"
    )
    QueryInvestigationResult investigate(
        @Name("queryToBeInvestigated")
        @Description("Query written in the JCR-SQL2 query language that should be investigated")
        String queryToBeInvestigated
    );
}
