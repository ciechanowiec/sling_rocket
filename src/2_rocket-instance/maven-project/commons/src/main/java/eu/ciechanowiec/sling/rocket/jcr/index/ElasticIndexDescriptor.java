package eu.ciechanowiec.sling.rocket.jcr.index;

import java.util.Set;

class ElasticIndexDescriptor implements IndexDescriptor {

    @Override
    public String indexClass() {
        return "org.apache.jackrabbit.oak.plugins.index.elastic.query.ElasticIndex";
    }

    @Override
    public String idInPlanStartsWith() {
        return "elasticsearch:";
    }

    @Override
    public Set<String> possibleNames() {
        return Set.of("elasticsearch");
    }
}
