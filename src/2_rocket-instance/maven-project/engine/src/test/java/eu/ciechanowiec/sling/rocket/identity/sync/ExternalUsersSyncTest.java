package eu.ciechanowiec.sling.rocket.identity.sync;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.jackrabbit.oak.spi.security.authentication.external.impl.jmx.SynchronizationMBean;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalUsersSyncTest extends TestEnvironment {

    ExternalUsersSyncTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @Test
    void test() {
        SynchronizationMBean syncMBeanOne = new SynchronizationMBeanMock("user-1", "user-2", "user-3");
        SynchronizationMBean syncMBeanTwo = new SynchronizationMBeanMock("user-4", "user-5", "user-6");
        context.registerService(SynchronizationMBean.class, syncMBeanOne);
        context.registerService(SynchronizationMBean.class, syncMBeanTwo);
        ExternalUsersSync externalUsersSync = context.registerInjectActivateService(
            ExternalUsersSync.class, Map.of("schedule-cycle.cron-expression", "0 0 4 * * ?")
        );
        assertEquals(
            List.of("user-4", "user-5", "user-6", "user-1", "user-2", "user-3"),
            externalUsersSync.syncAllExternalUsers()
        );
    }
}
