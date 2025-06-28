package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import java.time.YearMonth;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
class StagedMonthNode implements WithJCRPath {

    private final JCRPath jcrPath;
    private final YearMonth yearMonth;

    StagedMonthNode(ParentJCRPath parentJCRPath, YearMonth yearMonth) {
        this.jcrPath = new TargetJCRPath(parentJCRPath, yearMonth.toString());
        this.yearMonth = yearMonth;
    }

    @SneakyThrows
    void stageForSaving(ResourceResolver resourceResolver) {
        Resource monthResource = ResourceUtil.getOrCreateResource(
            resourceResolver, jcrPath().get(), Map.of(
                JcrConstants.JCR_PRIMARYTYPE, MonthNode.NT_MONTH,
                MonthNode.PN_MONTH, yearMonth.getMonthValue()
            ), null, false
        );
        log.trace("Staged {}", monthResource);
        int firstDayInTheMonth = 1;
        int lastDayInTheMonth = yearMonth.lengthOfMonth();
        IntStream.rangeClosed(firstDayInTheMonth, lastDayInTheMonth)
            .mapToObj(yearMonth::atDay)
            .map(localDate -> new StagedDayNode(new ParentJCRPath(jcrPath), localDate))
            .forEach(stagedDayNode -> stagedDayNode.stageForSaving(resourceResolver));
    }

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }
}
