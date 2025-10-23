package eu.ciechanowiec.sling.rocket.job;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

/**
 * Simplified API wrapper over the {@link Scheduler}.
 */
@Component(
    service = SimpleScheduler.class,
    immediate = true
)
@ServiceDescription("Simplified API wrapper over the Scheduler")
@Slf4j
@ToString
public class SimpleScheduler {

    private final Scheduler scheduler;

    /**
     * Constructs an instance of this class.
     *
     * @param scheduler {@link Scheduler} that will be used by the constructed object to schedule related
     *                  {@link SchedulableRunnable} instances
     */
    @Activate
    public SimpleScheduler(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        Scheduler scheduler
    ) {
        this.scheduler = scheduler;
    }

    @SuppressWarnings("WeakerAccess")
    boolean schedule(SchedulableRunnable schedulableRunnable) {
        String name = schedulableRunnable.id().get();
        log.info("Scheduling {} by this name: {}", schedulableRunnable, name);
        String cronExpression = schedulableRunnable.scheduleCycleCronExpression();
        ScheduleOptions scheduleOptions = scheduler.EXPR(cronExpression);
        scheduleOptions.name(name);
        boolean wasScheduled = scheduler.schedule(schedulableRunnable, scheduleOptions);
        if (wasScheduled) {
            log.debug("Scheduled {} by this name: {}", schedulableRunnable, name);
        } else {
            log.warn("Failed to schedule {} by this name: {}", schedulableRunnable, name);
        }
        return wasScheduled;
    }

    @SuppressWarnings("WeakerAccess")
    boolean unschedule(SchedulableRunnableID schedulableRunnableID) {
        String nameUnwrapped = schedulableRunnableID.get();
        log.info("Unscheduling by this name: {}", nameUnwrapped);
        boolean wasUnscheduled = scheduler.unschedule(nameUnwrapped);
        if (wasUnscheduled) {
            log.debug("Unscheduled by this name: {}", nameUnwrapped);
        } else {
            log.warn("Failed to unschedule by this name: {}", nameUnwrapped);
        }
        return wasUnscheduled;
    }
}
