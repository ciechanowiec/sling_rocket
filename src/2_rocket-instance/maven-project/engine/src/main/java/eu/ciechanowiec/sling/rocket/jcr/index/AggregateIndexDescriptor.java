package eu.ciechanowiec.sling.rocket.jcr.index;

import org.apache.jackrabbit.oak.plugins.index.aggregate.AggregateIndex;

import java.util.Set;

class AggregateIndexDescriptor implements IndexDescriptor {

    @Override
    public String indexClass() {
        return AggregateIndex.class.getName();
    }

    @Override
    public String idInPlanStartsWith() {
        return "aggregate ";
    }

    @Override
    public Set<String> possibleNames() {
        String elasticName = "aggregate %s".formatted("elasticsearch");
        String luceneName = "aggregate %s".formatted("lucene");
        String lucenePropertyName = "aggregate %s".formatted("lucene-property");
        return Set.of("aggregate no-index", elasticName, luceneName, lucenePropertyName);
    }
}
