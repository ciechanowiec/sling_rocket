package eu.ciechanowiec.sling.rocket.identity.sync;

import eu.ciechanowiec.sling.rocket.job.SchedulableJobConsumer;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.apache.jackrabbit.oak.spi.security.authentication.external.impl.jmx.SynchronizationMBean;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Triggers the {@link SynchronizationMBean#syncAllExternalUsers()} method on all bounded
 * {@link SynchronizationMBean}-s.
 * <p>
 * Triggering operation can be scheduled via setting the {@link SchedulableJobConsumer#CRON_EXPRESSION_PROPERTY}
 * property.
 */
@Component(
    service = {ExternalUsersSync.class, ExternalUsersSyncMBean.class, SchedulableJobConsumer.class, JobConsumer.class},
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        "jmx.objectname=eu.ciechanowiec.sling.rocket.engine:type=Identity Management,name=External Users Sync",
        JobConsumer.PROPERTY_TOPICS + "=" + "eu/ciechanowiec/sling/rocket/identity/sync/EXTERNAL_USERS_SYNC"
    }
)
@Slf4j
@ServiceDescription(ExternalUsersSync.SERVICE_DESCRIPTION)
@ToString
public class ExternalUsersSync extends AnnotatedStandardMBean implements ExternalUsersSyncMBean,
                                                                         SchedulableJobConsumer {

    static final String SERVICE_DESCRIPTION
        = "Triggers the syncAllExternalUsers() method on all bounded SynchronizationMBean-s";

    @Reference(
        cardinality = ReferenceCardinality.AT_LEAST_ONE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private final Collection<SynchronizationMBean> syncMBeans;

    /**
     * Constructs an instance of this class.
     */
    @Activate
    public ExternalUsersSync() {
        super(ExternalUsersSyncMBean.class);
        syncMBeans = new ArrayList<>();
        log.info("Activated {}", this);
    }

    @Override
    public List<String> syncAllExternalUsers() {
        log.debug("Sync triggered");
        return syncMBeans.stream()
            .map(
                synchronizationMBean -> {
                    String idpName = synchronizationMBean.getIDPName();
                    log.info("Starting sync for IDP: {}", idpName);
                    List<String> resultMessages = List.of(synchronizationMBean.syncAllExternalUsers());
                    log.info("Finished sync for IDP: {}. Result messages: {}", idpName, resultMessages);
                    return resultMessages;
                }
            ).flatMap(Collection::stream)
            .toList();
    }

    @Override
    public JobResult process(Job job) {
        syncAllExternalUsers();
        return JobResult.OK;
    }
}
