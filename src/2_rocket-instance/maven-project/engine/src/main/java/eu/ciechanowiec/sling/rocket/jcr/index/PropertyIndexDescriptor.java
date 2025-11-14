package eu.ciechanowiec.sling.rocket.jcr.index;

import java.util.Set;

class PropertyIndexDescriptor implements IndexDescriptor {

    @Override
    public String indexClass() {
        return "org.apache.jackrabbit.oak.plugins.index.property.PropertyIndex";
    }

    @Override
    public String idInPlanStartsWith() {
        return "property ";
    }

    @Override
    public Set<String> possibleNames() {
        return Set.of("property");
    }
}
