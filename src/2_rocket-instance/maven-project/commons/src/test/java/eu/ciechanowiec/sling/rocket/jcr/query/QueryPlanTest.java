package eu.ciechanowiec.sling.rocket.jcr.query;

import eu.ciechanowiec.sling.rocket.jcr.index.IndexDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryPlanTest {

    @Test
    void shouldReturnLucenePropertyIndexDescriptor() {
        String rawPlan = """
                [crm:client] as [client] /* lucene:clientFullName
                    indexDefinition: /oak:index/clientFullName
                    estimatedEntries: 1871
                    luceneQuery: crm:clientFullName:John Doe
                */ cost: { "client": { perEntry: 1.0, perExecution: 1.0, count: 1871 } }""";
        QueryPlan queryPlan = new QueryPlan(rawPlan);
        IndexDescriptor result = queryPlan.indexDescriptor();
        assertEquals("org.apache.jackrabbit.oak.plugins.index.lucene.LucenePropertyIndex", result.indexClass());
    }

    @Test
    void shouldReturnNodeTypeIndexDescriptor() {
        String rawPlan = """
                [crm:client] as [client] /* nodeType
                    path: /content/crm/clients
                    primaryTypes: [crm:client]
                    mixinTypes: []
                */ cost: { "client": 3360.0 }""";
        QueryPlan queryPlan = new QueryPlan(rawPlan);
        IndexDescriptor result = queryPlan.indexDescriptor();
        assertEquals("org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndex", result.indexClass());
    }

    @Test
    void shouldReturnPropertyIndexDescriptor() {
        String rawPlan = """
                [nt:base] as [node] /* property slingResourceType
                    indexDefinition: /oak:index/slingResourceType
                    values: 'crm/client'
                    estimatedCost: 2.0
                */ cost: { "node": 2.0 }""";
        QueryPlan queryPlan = new QueryPlan(rawPlan);
        IndexDescriptor result = queryPlan.indexDescriptor();
        assertEquals("org.apache.jackrabbit.oak.plugins.index.property.PropertyIndex", result.indexClass());
    }

    @Test
    void shouldReturnTraversingIndexDescriptor() {
        String rawPlan = """
                [nt:base] as [nt:base] /* traverse
                    allNodes (warning: slow)
                    estimatedEntries: 8292.0
                 */ cost: { "nt:base": 8292.0 }""";
        QueryPlan queryPlan = new QueryPlan(rawPlan);
        IndexDescriptor result = queryPlan.indexDescriptor();
        assertEquals("org.apache.jackrabbit.oak.query.index.TraversingIndex", result.indexClass());
    }

    @Test
    void shouldThrowExceptionOnEmptyPlan() {
        String rawPlan = "";
        QueryPlan queryPlan = new QueryPlan(rawPlan);
        assertThrows(IllegalArgumentException.class, queryPlan::indexDescriptor);
    }

    @Test
    void shouldThrowExceptionOnPlanWithoutMarker() {
        String rawPlan = "[nt:base] as [nt:base]";
        QueryPlan queryPlan = new QueryPlan(rawPlan);
        assertThrows(IllegalArgumentException.class, queryPlan::indexDescriptor);
    }

    @Test
    void shouldThrowExceptionOnUnknownIndex() {
        String rawPlan = "[nt:base] as [nt:base] /* unknown:index */";
        QueryPlan queryPlan = new QueryPlan(rawPlan);
        assertThrows(IllegalStateException.class, queryPlan::indexDescriptor);
    }
}
