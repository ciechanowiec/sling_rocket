package eu.ciechanowiec.sling.rocket.jcr.query;

import eu.ciechanowiec.sling.rocket.jcr.index.IndexDescriptor;
import eu.ciechanowiec.sling.rocket.jcr.index.IndexDescriptorWithQueryCost;
import eu.ciechanowiec.sling.rocket.jcr.index.IndexDescriptors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
class QueryCostFromQueryLogs {

    private final List<Pattern> costLogPatterns;
    private final QueryLogs queryLogs;

    QueryCostFromQueryLogs(QueryLogs queryLogs) {
        // Patterns from `org.apache.jackrabbit.oak.query.QueryImpl`:
        this.costLogPatterns = List.of(
            Pattern.compile("cost for (.*?) is (.*)\n"),
            Pattern.compile("cost for \\[/.*?] of type \\((.*?)\\) with plan \\[.*?] is (.*)\n", Pattern.DOTALL)
        );
        this.queryLogs = queryLogs;
    }

    List<IndexDescriptorWithQueryCost> queryCostPerIndex(String queryToBeAssessed) {
        return queryLogs.logsForQuery(queryToBeAssessed)
            .stream()
            .flatMap(
                logLine -> costLogPatterns.stream()
                    .map(pattern -> pattern.matcher(logLine))
                    .filter(Matcher::matches)
            ).collect(
                Collectors.toMap(
                    matcher -> matchingIndexDescriptor(matcher.group(1).trim()),
                    matcher -> matcher.group(2).trim(),
                    (existingValue, newValue) -> {
                        throw new IllegalStateException(
                            String.format(
                                "Duplicate key found for index name. Values: '%s' and '%s'", existingValue, newValue)
                        );
                    }
                )
            ).entrySet()
            .stream()
            .map(entry -> new IndexDescriptorWithQueryCost(entry.getKey(), entry.getValue()))
            .sorted(new CostAwareComparator())
            .toList();
    }

    private IndexDescriptor matchingIndexDescriptor(String searchedIndexName) {
        IndexDescriptor indexDescriptor = new IndexDescriptors().all()
            .stream()
            .filter(descriptor -> descriptor.possibleNames().contains(searchedIndexName))
            .findFirst()
            .orElse(
                new IndexDescriptor() {

                    @Override
                    public String indexClass() {
                        return "unknown";
                    }

                    @Override
                    public String idInPlanStartsWith() {
                        throw new NotImplementedException("Unknown index");
                    }

                    @Override
                    public Set<String> possibleNames() {
                        return Set.of("unknown");
                    }
                }
            );
        log.trace("'{}' matched with {}", searchedIndexName, indexDescriptor);
        return indexDescriptor;
    }

    @SuppressWarnings({"ReturnCount", "PMD.CognitiveComplexity"})
    private static final class CostAwareComparator implements Comparator<IndexDescriptorWithQueryCost> {
        @Override
        public int compare(IndexDescriptorWithQueryCost o1, IndexDescriptorWithQueryCost o2) {
            String cost1 = o1.queryCost();
            String cost2 = o2.queryCost();
            boolean isNumber1 = NumberUtils.isCreatable(cost1);
            boolean isNumber2 = NumberUtils.isCreatable(cost2);
            if (isNumber1 && isNumber2) {
                return Double.compare(Double.parseDouble(cost1), Double.parseDouble(cost2));
            } else if (isNumber1) {
                return -1;
            } else if (isNumber2) {
                return 1;
            } else if (cost1.equals(cost2)) {
                return o1.indexClass().compareTo(o2.indexClass());
            } else {
                return cost1.compareTo(cost2);
            }
        }
    }
}
