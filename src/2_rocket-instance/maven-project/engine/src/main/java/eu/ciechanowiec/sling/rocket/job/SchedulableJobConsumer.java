package eu.ciechanowiec.sling.rocket.job;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.quartz.CronExpression;

import java.util.Map;

/**
 * {@link JobConsumer} scheduled for periodic execution.
 * <ol>
 *     <li>The {@link SchedulableJobConsumer} is scheduled for periodic execution automatically by the
 *     {@link SchedulableJobConsumers}.</li>
 *     <li>The execution schedule is determined by the {@link SchedulableJobConsumer#CRON_EXPRESSION_PROPERTY}
 *     OSGi {@link Component#property()} set on the {@link SchedulableJobConsumer}.</li>
 *     <li>Every {@link SchedulableJobConsumer} must have a valid
 *     {@link SchedulableJobConsumer#CRON_EXPRESSION_PROPERTY} property set. Otherwise, the
 *     {@link SchedulableJobConsumer} won't be scheduled for execution.</li>
 *     <li>The scheduling means that at the specified moment a {@link Job} for every topic specified in the
 *     {@link JobConsumer#PROPERTY_TOPICS} OSGi {@link Component#property()} set on this {@link SchedulableJobConsumer}
 *     will be submitted for execution via the {@link org.apache.sling.event.jobs.JobManager#addJob(String, Map)}.
 *     Therefore, every {@link SchedulableJobConsumer} must have a valid
 *     {@link JobConsumer#PROPERTY_TOPICS} property set.</li>
 * </ol>
 */
@FunctionalInterface
@SuppressWarnings({"WeakerAccess", "InterfaceNeverImplemented", "InterfaceIsType", "PMD.ConstantsInInterface"})
public interface SchedulableJobConsumer extends JobConsumer {

    /**
     * Name of the OSGi {@link Component#property()} that defines the schedule cycle of the
     * {@link SchedulableJobConsumer}. The value of the property must be a {@link String} that defines a valid
     * {@link CronExpression}.
     * <p>
     * <i>Examples:</i>
     * <ol>
     *  <li>{@code 0 * * * * ?} - every minute</li>
     *  <li>{@code 0 0 4 * * ?} - every day at 4:00 a.m.</li>
     * </ol>
     */
    String CRON_EXPRESSION_PROPERTY = "schedule-cycle.cron-expression";
}
