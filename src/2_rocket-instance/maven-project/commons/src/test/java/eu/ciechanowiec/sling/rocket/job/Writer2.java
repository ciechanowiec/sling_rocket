package eu.ciechanowiec.sling.rocket.job;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;

@Component(
    service = {SchedulableJobConsumer.class, JobConsumer.class},
    immediate = true,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + Writer2.JOB_TOPIC,
        SchedulableJobConsumer.CRON_EXPRESSION_PROPERTY + "=" + "0 */5 * * * ?"
    }
)
@Slf4j
@ToString
public class Writer2 implements SchedulableJobConsumer {

    static final String JOB_TOPIC = "eu/ciechanowiec/slexamplus/job/writer2";

    @Override
    public JobResult process(Job job) {
        log.info("Processing {}", job);
        return JobResult.OK;
    }
}
