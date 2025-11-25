package eu.ciechanowiec.sling.rocket.jcr.query;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.TooManyStaticImports")
class QueryInvestigationTest extends TestEnvironment {

    QueryInvestigationTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    @SuppressWarnings({"LineLength", "MethodLength"})
    void mustReturnQueryInvestigationResult() {
        context.build().resource("/content/someClient", Map.of("clientFullName", "Some Client")).commit();
        QueryLogsInterception queryLogsInterception = spy(new QueryLogsInterception());
        when(queryLogsInterception.savedILoggingEvents(anyString())).thenReturn(
            List.of(
                "cost for reference is Infinity\n",
                "cost for property is Infinity\n",
                "cost for nodeType is 14.0\n",
                "cost for traverse is 1.0E7\n",
                """
                    cost for [/oak:index/clientFullName] of type (lucene-property) with plan [lucene:clientFullName
                        indexDefinition: /oak:index/clientFullName
                        estimatedEntries: 16356
                        luceneQuery: *:*
                    ] is 9357.00
                    """
            )
        );
        context.registerInjectActivateService(queryLogsInterception);
        context.registerInjectActivateService(QueryLogs.class);
        QueryInvestigation queryInvestigation = context.registerInjectActivateService(QueryInvestigation.class);
        String query
            = "SELECT [jcr:uuid] FROM [nt:unstructured] AS client WHERE ISDESCENDANTNODE(client, '/content') AND client.[clientFullName] = 'Some Client'";
        String expectedQueryInvestigationPart = """
            QUERY:
            SELECT [jcr:uuid] FROM [nt:unstructured] AS client WHERE ISDESCENDANTNODE(client, '/content') AND client.[clientFullName] = 'Some Client'

            QUERY PLAN:
            [nt:unstructured] as [client] /* nodeType
                path: /content
                primaryTypes: [nt:unstructured, rep:root]
                mixinTypes: []
             */ cost: { "client": 8.0 }

            INDEX CLASS IN QUERY PLAN:
            org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndex

            QUERY COST PER INDEX:
             - Query cost for 'org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndex' [nodeType]: 14.0
             - Query cost for 'org.apache.jackrabbit.oak.plugins.index.lucene.LucenePropertyIndex' [lucene-property]: 9357.00
             - Query cost for 'org.apache.jackrabbit.oak.query.index.TraversingIndex' [traverse]: 1.0E7
             - Query cost for 'org.apache.jackrabbit.oak.plugins.index.property.PropertyIndex' [property]: Infinity
             - Query cost for 'org.apache.jackrabbit.oak.plugins.index.reference.ReferenceIndex' [reference]: Infinity

            QUERY PERFORMANCE RESULT:
              QUERY: SELECT [jcr:uuid] FROM [nt:unstructured] AS client WHERE ISDESCENDANTNODE(client, '/content') AND client.[clientFullName] = 'Some Client'
              PAGE SIZE: 20
              FIRST PAGE RESULTS:
              - /content/someClient
              1. TOTAL NUMBER OF RESULTS:       1""";
        String queryInvestigationResult = queryInvestigation.investigate(query).toString();
        assertAll(
            () -> assertTrue(queryInvestigationResult.contains(expectedQueryInvestigationPart)),
            () -> assertTrue(queryInvestigationResult.contains("2. QUERY EXECUTION TIME:          ")),
            () -> assertTrue(queryInvestigationResult.contains("[Query#execute()]")),
            () -> assertTrue(queryInvestigationResult.contains("3. GET NODES TIME:                ")),
            () -> assertTrue(queryInvestigationResult.contains("[QueryResult#getNodes()]")),
            () -> assertTrue(queryInvestigationResult.contains("4. READ NODES IN FIRST PAGE TIME: ")),
            () -> assertTrue(queryInvestigationResult.contains("[NodeIterator#next() * 20]")),
            () -> assertTrue(queryInvestigationResult.contains("5. READ ALL NODES TIME:           ")),
            () -> assertTrue(queryInvestigationResult.contains("[NodeIterator#next() * 1]")),
            () -> assertTrue(queryInvestigationResult.contains("6. TOTAL TIME:                    "))
        );
    }
}
