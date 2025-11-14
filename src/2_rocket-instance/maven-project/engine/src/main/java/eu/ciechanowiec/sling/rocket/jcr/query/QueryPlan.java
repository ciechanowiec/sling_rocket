package eu.ciechanowiec.sling.rocket.jcr.query;

import eu.ciechanowiec.sling.rocket.jcr.index.IndexDescriptor;
import eu.ciechanowiec.sling.rocket.jcr.index.IndexDescriptors;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
class QueryPlan {

    private final String rawPlan;

    QueryPlan(String rawPlan) {
        this.rawPlan = rawPlan;
    }

    IndexDescriptor indexDescriptor() {
        String indexIdFromPlan = indexIdFromPlan();
        IndexDescriptors indexDescriptors = new IndexDescriptors();
        return indexDescriptors.all().stream()
            .filter(
                descriptor -> {
                    String prefix = descriptor.idInPlanStartsWith();
                    return indexIdFromPlan.startsWith(prefix);
                }
            ).findFirst()
            .orElseThrow(() -> new IllegalStateException("Unknown index type for plan: %s".formatted(rawPlan)));
    }

    private String indexIdFromPlan() {
        String marker = " /* ";
        String firstLine = rawPlan.lines()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Raw plan is empty or invalid."));
        return Optional.of(firstLine.indexOf(marker))
            .filter(markerIndex -> markerIndex != -1)
            .map(markerIndex -> firstLine.substring(markerIndex + marker.length()))
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Cannot find '%s' marker in the first line of the plan.".formatted(marker)
                )
            );
    }

    @Override
    public String toString() {
        return rawPlan;
    }
}
