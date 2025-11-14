package eu.ciechanowiec.sling.rocket.jcr.index;

import javax.jcr.query.Query;
import java.util.Set;

/**
 * {@link IndexDescriptor} with a related cost of a specific {@link Query}.
 */
public class IndexDescriptorWithQueryCost implements IndexDescriptor {

    private final IndexDescriptor indexDescriptor;
    private final String queryCost;

    /**
     * Constructs an instance of this class.
     *
     * @param indexDescriptor {@link IndexDescriptor} represented by this class
     * @param queryCost       cost of a specific {@link Query} related to the {@link IndexDescriptor} represented by
     *                        this class
     */
    public IndexDescriptorWithQueryCost(IndexDescriptor indexDescriptor, String queryCost) {
        this.indexDescriptor = indexDescriptor;
        this.queryCost = queryCost;
    }

    @Override
    public String toString() {
        return "Query cost for '%s' %s: %s".formatted(
            indexClass(),
            possibleNames(),
            queryCost
        );
    }

    /**
     * Returns the cost of a specific {@link Query} related to the {@link IndexDescriptor} represented by this class.
     *
     * @return cost of a specific {@link Query} related to the {@link IndexDescriptor} represented by this class
     */
    public String queryCost() {
        return queryCost;
    }

    @Override
    public String indexClass() {
        return indexDescriptor.indexClass();
    }

    @Override
    public String idInPlanStartsWith() {
        return indexDescriptor.idInPlanStartsWith();
    }

    @Override
    public Set<String> possibleNames() {
        return indexDescriptor.possibleNames();
    }
}
