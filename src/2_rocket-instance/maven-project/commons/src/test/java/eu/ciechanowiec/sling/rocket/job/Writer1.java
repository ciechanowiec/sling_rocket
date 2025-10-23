package eu.ciechanowiec.sling.rocket.job;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component(
    service = {SchedulableJobConsumer.class, JobConsumer.class},
    immediate = true,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + Writer1.JOB_TOPIC_AAA,
        JobConsumer.PROPERTY_TOPICS + "=" + Writer1.JOB_TOPIC_BBB
    }
)
@Slf4j
@ToString
@Designate(
    ocd = Writer1Config.class
)
public class Writer1 implements SchedulableJobConsumer {

    static final String JOB_TOPIC_AAA = "eu/ciechanowiec/slexamplus/job/writer1-AAA";
    static final String JOB_TOPIC_BBB = "eu/ciechanowiec/slexamplus/job/writer1-BBB";

    private final AtomicReference<Writer1Config> config;
    private final AtomicInteger counter;

    @Activate
    public Writer1(Writer1Config config) {
        this.config = new AtomicReference<>(config);
        counter = new AtomicInteger(0);
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(Writer1Config config) {
        this.config.set(config);
        log.info("Configured {}", this);
    }

    @Override
    public JobResult process(Job job) {
        counter.incrementAndGet();
        log.info("Processing {}. Counter: {}", job, counter.get());
        return JobResult.OK;
    }
}
