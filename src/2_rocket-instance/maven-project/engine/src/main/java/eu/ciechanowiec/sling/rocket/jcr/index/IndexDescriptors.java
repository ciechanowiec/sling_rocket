package eu.ciechanowiec.sling.rocket.jcr.index;

import java.util.Collection;
import java.util.List;

/**
 * Repository of all known {@link IndexDescriptor}s.
 */
public class IndexDescriptors {

    /**
     * Constructs an instance of this class.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public IndexDescriptors() {
        // constructor for Javadoc
    }

    /**
     * Returns all known {@link IndexDescriptor}s.
     *
     * @return all known {@link IndexDescriptor}s
     */
    public Collection<IndexDescriptor> all() {
        return List.of(
            new AggregateIndexDescriptor(),
            new ElasticIndexDescriptor(),
            new LucenePropertyIndexDescriptor(),
            new NodeTypeIndexDescriptor(),
            new PropertyIndexDescriptor(),
            new ReferenceIndexDescriptor(),
            new TraversingIndexDescriptor()
        );
    }
}
