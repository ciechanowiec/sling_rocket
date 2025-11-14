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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

@Slf4j
class StagedDayNode implements WithJCRPath {

    private final JCRPath jcrPath;
    private final LocalDate localDate;

    StagedDayNode(ParentJCRPath parentJCRPath, LocalDate localDate) {
        this.jcrPath = new TargetJCRPath(parentJCRPath, localDate.toString());
        this.localDate = localDate;
    }

    @SneakyThrows
    void stageForSaving(ResourceResolver resourceResolver) {
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault());
        Calendar calendar = GregorianCalendar.from(zonedDateTime);
        Resource dayResource = ResourceUtil.getOrCreateResource(
            resourceResolver, jcrPath().get(), Map.of(
                JcrConstants.JCR_PRIMARYTYPE, DayNode.NT_DAY,
                DayNode.PN_DAY, calendar
            ), null, false
        );
        log.trace("Staged {}", dayResource);
    }

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }
}
