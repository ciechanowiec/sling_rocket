package eu.ciechanowiec.sling.rocket.jcr.index;

import java.util.Set;

class NodeTypeIndexDescriptor implements IndexDescriptor {

    @Override
    public String indexClass() {
        return "org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndex";
    }

    @Override
    public String idInPlanStartsWith() {
        return "nodeType";
    }

    @Override
    public Set<String> possibleNames() {
        return Set.of("nodeType");
    }
}
