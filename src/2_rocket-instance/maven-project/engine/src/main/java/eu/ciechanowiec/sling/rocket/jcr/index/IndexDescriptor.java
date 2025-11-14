package eu.ciechanowiec.sling.rocket.jcr.index;

import org.apache.jackrabbit.oak.plugins.index.aggregate.AggregateIndex;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexPlan;
import org.apache.jackrabbit.oak.plugins.index.search.spi.query.FulltextIndex;
import org.apache.jackrabbit.oak.spi.query.QueryIndex;
import org.apache.jackrabbit.oak.spi.state.NodeState;

import javax.jcr.query.Query;
import java.util.Set;

/**
 * Descriptor of a specific {@link QueryIndex} implementation.
 */
public interface IndexDescriptor {

    /**
     * {@link Class#getName()} for the {@link QueryIndex} implementation described by this {@link IndexDescriptor}.
     *
     * @return {@link Class#getName()} for the {@link QueryIndex} implementation described by this
     * {@link IndexDescriptor}
     */
    String indexClass();

    /**
     * When the {@code EXPLAIN MEASURE} command (see
     * <a href="https://jackrabbit.apache.org/oak/docs/query/grammar-sql2.html#explain">the documentation for
     * details</a>) on the specified {@link Query} in the {@link Query#JCR_SQL2} query language is run, a plan for that
     * {@link Query} as a multiline {@link String} is returned. The first line of the plan contains a single " /* "
     * sequence of characters (leading and trailing whitespaces included). Immediately after that sequence, in the same
     * line, there is an identifier of a {@link QueryIndex} used for the specified {@link Query}. The identifier ends
     * with the end of the line. This method returns a {@link String} that the identifier of the {@link QueryIndex}
     * described by this {@link IndexDescriptor} starts with or is equal to.
     * <h4>{@link AggregateIndexDescriptor}</h4>
     * For the {@link AggregateIndexDescriptor} the starting {@link String} returned by this method is "aggregate "
     * (trailing whitespace included).
     * <p>
     * See {@link AggregateIndex#getPlanDescription(QueryIndex.IndexPlan, NodeState)} for details.
     * <h4>{@link ElasticIndexDescriptor}</h4>
     * For the {@link ElasticIndexDescriptor} the starting {@link String} returned by this method is "elasticsearch:"
     * (trailing colon included).
     * <p>
     * See {@link FulltextIndex#getPlanDescription(QueryIndex.IndexPlan, NodeState)} and
     * {@code org.apache.jackrabbit.oak.plugins.index.elastic.query.ElasticIndex#getType()} for details.
     * <h4>{@link LucenePropertyIndexDescriptor}</h4>
     * For the {@link LucenePropertyIndexDescriptor} the starting {@link String} returned by this method is "lucene:"
     * (trailing colon included).
     * <p>
     * See {@link FulltextIndex#getPlanDescription(QueryIndex.IndexPlan, NodeState)} and
     * {@code org.apache.jackrabbit.oak.plugins.index.lucene.LucenePropertyIndex#getType()} for details.
     * <p>
     * Example of a plan for the {@link LucenePropertyIndexDescriptor}, where the identifier of the {@link QueryIndex}
     * described by the {@link LucenePropertyIndexDescriptor} is highlighted in bold ({@code lucene:clientFullName}):
     * <pre>
     * [crm:client] as [client] /* <strong>lucene:clientFullName</strong>
     *      indexDefinition: /oak:index/clientFullName
     *      estimatedEntries: 1871
     *      luceneQuery: crm:clientFullName:John Doe
     * *&#47; cost: { "client": { perEntry: 1.0, perExecution: 1.0, count: 1871 } }
     * </pre>
     * <h4>{@link NodeTypeIndexDescriptor}</h4>
     * <p>
     * For the {@link NodeTypeIndexDescriptor} the starting {@link String} returned by this method is "nodeType".
     * <p>
     * See {@code org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndex#getPlan(Filter, NodeState)} for
     * details.
     * <p>
     * Example of a plan for the {@link NodeTypeIndexDescriptor}, where the identifier of the {@link QueryIndex}
     * described by the {@link NodeTypeIndexDescriptor} is highlighted in bold ({@code nodeType}):
     *
     * <pre>
     * [crm:client] as [client] /* <strong>nodeType</strong>
     *      path: /content/crm/clients
     *      primaryTypes: [crm:client]
     *      mixinTypes: []
     * *&#47; cost: { "client": 3360.0 }
     * </pre>
     *
     * <h4>{@link PropertyIndexDescriptor}</h4>
     * <p>
     * For the {@link PropertyIndexDescriptor} the starting {@link String} returned by this method is "property "
     * (trailing whitespace included).
     * <p>
     * See {@link PropertyIndexPlan#toString()} for details.
     * <p>
     * Example of a plan for the {@link PropertyIndexDescriptor}, where the identifier of the {@link QueryIndex}
     * described by the {@link PropertyIndexDescriptor} is highlighted in bold ({@code property slingResourceType}):
     * <pre>
     * [nt:base] as [node] /* <strong>property slingResourceType</strong>
     *      indexDefinition: /oak:index/slingResourceType
     *      values: 'crm/client'
     *      estimatedCost: 2.0
     * *&#47; cost: { "node": 2.0 }
     * </pre>
     *
     * <h4>{@link ReferenceIndexDescriptor}</h4>
     * <p>
     * For the {@link ReferenceIndexDescriptor} the starting {@link String} returned by this method is "reference".
     * <p>
     * See {@code org.apache.jackrabbit.oak.plugins.index.reference.ReferenceIndex#getPlan(Filter, NodeState)} for
     * details.
     *
     * <h4>{@link TraversingIndexDescriptor}</h4>
     * <p>
     * For the {@link TraversingIndexDescriptor} the starting {@link String} returned by this method is "traverse".
     * <p>
     * See {@code org.apache.jackrabbit.oak.query.index.TraversingIndex#getPlan(Filter, NodeState)} for details.
     * <p>
     * Example of a plan for the {@link TraversingIndexDescriptor}, where the identifier of the {@link QueryIndex}
     * described by the {@link TraversingIndexDescriptor} is highlighted in bold ({@code traverse}):
     * <pre>
     * [nt:base] as [nt:base] /* <strong>traverse</strong>
     *      allNodes (warning: slow)
     *      estimatedEntries: 8292.0
     * *&#47; cost: { "nt:base": 8292.0 }
     * </pre>
     *
     * @return used in the plan {@link String} that the identifier of the {@link QueryIndex} described by this
     * {@link IndexDescriptor} starts with or is equal to
     */
    String idInPlanStartsWith();

    /**
     * Returns all possible values that can be returned by the {@link QueryIndex#getIndexName()} method of the
     * {@link QueryIndex} described by this {@link IndexDescriptor}.
     *
     * @return all possible values that can be returned by the {@link QueryIndex#getIndexName()} method of the
     * {@link QueryIndex} described by this {@link IndexDescriptor}
     */
    Set<String> possibleNames();
}
