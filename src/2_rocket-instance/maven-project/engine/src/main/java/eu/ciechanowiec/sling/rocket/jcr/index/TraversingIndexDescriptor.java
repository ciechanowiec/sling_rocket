package eu.ciechanowiec.sling.rocket.jcr.index;

import java.util.Set;

class TraversingIndexDescriptor implements IndexDescriptor {

    @Override
    public String indexClass() {
        return "org.apache.jackrabbit.oak.query.index.TraversingIndex";
    }

    @Override
    public String idInPlanStartsWith() {
        return "traverse";
    }

    @Override
    public Set<String> possibleNames() {
        return Set.of("traverse");
    }
}
