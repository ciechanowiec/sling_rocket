package eu.ciechanowiec.sling.rocket.jcr.index;

import org.apache.jackrabbit.oak.plugins.index.lucene.LucenePropertyIndex;

import java.util.Set;

class LucenePropertyIndexDescriptor implements IndexDescriptor {

    @Override
    public String indexClass() {
        return LucenePropertyIndex.class.getName();
    }

    @Override
    public String idInPlanStartsWith() {
        return "lucene:";
    }

    @Override
    public Set<String> possibleNames() {
        return Set.of("lucene-property");
    }
}
