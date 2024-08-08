package eu.ciechanowiec.sling.rocket.commons.activator;

import lombok.SneakyThrows;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import javax.management.MBeanServer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SlingContextExtension.class)
class CommonsBundleActivatorTest {

    private final SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @SneakyThrows
    @Test
    void testo() {
        MBeanServer initialService = context.getService(MBeanServer.class);
        assertNull(initialService);
        BundleContext bundleContext = context.bundleContext();
        BundleActivator commonsBundleActivator = new CommonsBundleActivator();
        commonsBundleActivator.start(bundleContext);
        MBeanServer finalService = context.getService(MBeanServer.class);
        assertNotNull(finalService);
    }
}
