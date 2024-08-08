package eu.ciechanowiec.sling.rocket.commons.activator;

import lombok.extern.slf4j.Slf4j;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

/**
 * Implementation of a {@link BundleActivator}.
 */
@Slf4j
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
@SuppressWarnings("PackageAccessibility")
public class CommonsBundleActivator implements BundleActivator {

    /**
     * Constructs an instance of this class.
     */
    @SuppressWarnings("WeakerAccess")
    public CommonsBundleActivator() {
        log.debug("Initialized {}", this);
    }

    /*
     * Ensures that OSGi services that are also MBeans are additionally registered in the MBeans Server:
     * - https://issues.apache.org/jira/browse/SLING-12367
     * - https://aries.apache.org/documentation/modules/jmx.html
     */
    @Override
    public void start(BundleContext context) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String mbsName = MBeanServer.class.getName();
        context.registerService(mbsName, mbs, null);
        log.info("Registered {}", mbsName);
    }

    @Override
    public void stop(BundleContext context) {
        log.info("Stopping the bundle");
    }
}
