package eu.ciechanowiec.sling.rocket.job;

import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.framework.ServiceReference;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

class JobTopics {

    private final Supplier<Collection<String>> topics;

    JobTopics(ServiceReference<SchedulableJobConsumer> serviceReference) {
        topics = () -> Optional.ofNullable(serviceReference.getProperty(JobConsumer.PROPERTY_TOPICS))
            .filter(String[].class::isInstance)
            .map(String[].class::cast)
            .map(List::of)
            .or(
                () -> Optional.ofNullable(serviceReference.getProperty(JobConsumer.PROPERTY_TOPICS))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(List::of)
            )
            .orElse(List.of());
    }

    Collection<String> get() {
        return topics.get();
    }

    @Override
    public String toString() {
        return "JobTopics{"
            + "topics=" + topics.get()
            + '}';
    }
}
