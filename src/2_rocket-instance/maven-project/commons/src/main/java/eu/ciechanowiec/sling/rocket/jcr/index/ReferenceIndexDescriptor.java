package eu.ciechanowiec.sling.rocket.jcr.index;

import org.apache.jackrabbit.oak.plugins.index.reference.NodeReferenceConstants;

import java.util.Set;

class ReferenceIndexDescriptor implements IndexDescriptor {

    @Override
    public String indexClass() {
        return "org.apache.jackrabbit.oak.plugins.index.reference.ReferenceIndex";
    }

    @Override
    public String idInPlanStartsWith() {
        return "reference";
    }

    @Override
    public Set<String> possibleNames() {
        return Set.of(NodeReferenceConstants.NAME);
    }
}
