package eu.ciechanowiec.sling.rocket.job;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.Map;
import java.util.Optional;

/**
 * Service that automatically schedules and unschedules {@link SchedulableJobConsumer} instances as they become
 * available, become unavailable or get properties updated in the OSGi service registry.
 */
@Component(
    service = SchedulableJobConsumers.class,
    immediate = true,
    reference =
    @Reference(
        name = "schedulableJobConsumers",
        bind = "bindSchedulableJobConsumers",
        unbind = "unbindSchedulableJobConsumers",
        updated = "updatedSchedulableJobConsumers",
        service = SchedulableJobConsumer.class,
        policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.AT_LEAST_ONE,
        policyOption = ReferencePolicyOption.GREEDY,
        target = "(" + SchedulableJobConsumer.CRON_EXPRESSION_PROPERTY + "=*)"
    )
)
@ServiceDescription(
    "Service that automatically schedules and unschedules SchedulableJobConsumer instances as they become "
        + "available, become unavailable or get properties updated in the OSGi service registry"
)
@ToString
@Slf4j
public class SchedulableJobConsumers {

    private final SimpleScheduler simpleScheduler;
    private final JobManager jobManager;

    /**
     * Constructs an instance of this class.
     *
     * @param simpleScheduler {@link SimpleScheduler} that will be used by the constructed object to schedule related
     *                        {@link SchedulableJobConsumer} instances
     * @param jobManager      {@link JobManager} that will be used by the constructed object to create related
     *                        {@link Job}s
     */
    @Activate
    public SchedulableJobConsumers(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        SimpleScheduler simpleScheduler,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        JobManager jobManager
    ) {
        this.simpleScheduler = simpleScheduler;
        this.jobManager = jobManager;
        log.info("Initialized {}", this);
    }

    @SuppressWarnings({"WeakerAccess", "squid:S1854"})
    void bindSchedulableJobConsumers(
        SchedulableJobConsumer schedulableJobConsumer, ServiceReference<SchedulableJobConsumer> serviceReference
    ) {
        log.info("Binding {} - {}", schedulableJobConsumer, serviceReference);
        SchedulableRunnable schedulableRunnable = new SchedulableRunnable() {

            @Override
            public void run() {
                log.trace("A job for '{}' - '{}' will be added", schedulableJobConsumer, serviceReference);
                JobTopics jobTopics = new JobTopics(serviceReference);
                jobTopics.get()
                    .stream()
                    .map(jobTopic -> Optional.ofNullable(jobManager.addJob(jobTopic, Map.of())))
                    .flatMap(Optional::stream)
                    .forEach(job -> log.debug("Added job: {}", job));
            }

            @Override
            public String scheduleCycleCronExpression() {
                return Optional.ofNullable(
                        serviceReference.getProperty(SchedulableJobConsumer.CRON_EXPRESSION_PROPERTY)
                    ).filter(String.class::isInstance)
                    .map(String.class::cast)
                    .orElseGet(
                        () -> {
                            log.warn(
                                "No '{}' property found for '{}'", SchedulableJobConsumer.CRON_EXPRESSION_PROPERTY,
                                schedulableJobConsumer
                            );
                            return StringUtils.EMPTY;
                        }
                    );
            }

            @Override
            public SchedulableRunnableID id() {
                return new SchedulableRunnableID(serviceReference);
            }
        };
        boolean wasScheduled = simpleScheduler.schedule(schedulableRunnable);
        log.info("Scheduled {}? Answer: {}", schedulableJobConsumer, wasScheduled);
    }

    @SuppressWarnings("WeakerAccess")
    void unbindSchedulableJobConsumers(
        SchedulableJobConsumer schedulableJobConsumer, ServiceReference<SchedulableJobConsumer> serviceReference
    ) {
        log.info("Unbinding {} - {}", schedulableJobConsumer, serviceReference);
        boolean wasUnscheduled = simpleScheduler.unschedule(new SchedulableRunnableID(serviceReference));
        log.info("Unscheduled {}? Answer: {}", schedulableJobConsumer, wasUnscheduled);
    }

    @SuppressWarnings("unused")
    void updatedSchedulableJobConsumers(
        SchedulableJobConsumer schedulableJobConsumer, ServiceReference<SchedulableJobConsumer> serviceReference
    ) {
        log.info("Updating {} - {}", schedulableJobConsumer, serviceReference);
        unbindSchedulableJobConsumers(schedulableJobConsumer, serviceReference);
        bindSchedulableJobConsumers(schedulableJobConsumer, serviceReference);
    }
}
