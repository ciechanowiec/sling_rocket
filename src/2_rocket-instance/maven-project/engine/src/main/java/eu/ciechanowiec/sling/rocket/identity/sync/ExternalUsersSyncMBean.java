package eu.ciechanowiec.sling.rocket.identity.sync;

import org.apache.jackrabbit.oak.api.jmx.Description;
import org.apache.jackrabbit.oak.spi.security.authentication.external.impl.jmx.SynchronizationMBean;

import java.util.List;

/**
 * MBean for a {@link ExternalUsersSync}.
 */
@FunctionalInterface
@Description(ExternalUsersSync.SERVICE_DESCRIPTION)
public interface ExternalUsersSyncMBean {

    /**
     * Triggers the {@link SynchronizationMBean#syncAllExternalUsers()} method on all bounded
     * {@link SynchronizationMBean}-s.
     *
     * @return combination of all result messages returned by the triggered
     * {@link SynchronizationMBean#syncAllExternalUsers()} methods
     */
    @Description(ExternalUsersSync.SERVICE_DESCRIPTION)
    List<String> syncAllExternalUsers();
}
