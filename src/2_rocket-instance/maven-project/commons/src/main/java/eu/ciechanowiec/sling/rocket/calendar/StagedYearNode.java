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

import java.time.Year;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
class StagedYearNode implements WithJCRPath {

    private final JCRPath jcrPath;
    private final Year year;

    StagedYearNode(ParentJCRPath parentJCRPath, Year year) {
        this.jcrPath = new TargetJCRPath(parentJCRPath, year.toString());
        this.year = year;
    }

    @SuppressWarnings("MagicNumber")
    @SneakyThrows
    void stageForSaving(ResourceResolver resourceResolver) {
        Resource yearResource = ResourceUtil.getOrCreateResource(
            resourceResolver, jcrPath().get(), Map.of(
                JcrConstants.JCR_PRIMARYTYPE, YearNode.NT_YEAR,
                YearNode.PN_YEAR, year.getValue()
            ), null, false
        );
        log.debug("Staged {}", yearResource);
        int firstMonthInTheYear = 1;
        int lastMonthInTheYear = 12;
        IntStream.rangeClosed(firstMonthInTheYear, lastMonthInTheYear)
            .mapToObj(year::atMonth)
            .map(yearMonth -> new StagedMonthNode(new ParentJCRPath(jcrPath), yearMonth))
            .forEach(stagedMonthNode -> stagedMonthNode.stageForSaving(resourceResolver));
    }

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }
}
