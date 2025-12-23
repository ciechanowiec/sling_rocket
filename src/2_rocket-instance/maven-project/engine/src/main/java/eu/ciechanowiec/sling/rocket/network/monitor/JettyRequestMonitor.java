package eu.ciechanowiec.sling.rocket.network.monitor;

import eu.ciechanowiec.sling.rocket.network.Request;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.servlet.whiteboard.Preprocessor;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Logs all information about the incoming HTTP requests on the Jetty level. Used for debugging purposes.
 */
@Component(
    immediate = true,
    service = {Preprocessor.class, JettyRequestMonitor.class},
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@ServiceDescription(
    "Logs all information about the incoming HTTP requests on the Jetty level. Used for debugging purposes"
)
@Designate(
    ocd = JettyRequestMonitorConfig.class
)
@ServiceRanking(2_000_000)
@Slf4j
@ToString
public class JettyRequestMonitor implements Preprocessor {

    private final AtomicBoolean isEnabled;

    /**
     * Constructs an instance of this class.
     *
     * @param config {@link JettyRequestMonitorConfig} that will be used by the constructed object
     */
    @Activate
    public JettyRequestMonitor(
        JettyRequestMonitorConfig config
    ) {
        this.isEnabled = new AtomicBoolean(config.is$_$enabled());
        log.debug("Initialized {}", this);
    }

    @Modified
    void configure(JettyRequestMonitorConfig config) {
        isEnabled.set(config.is$_$enabled());
        log.debug("Reconfigured {}", this);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("Initiated {}", this);
    }

    private void register(ServletRequest request) {
        boolean isHttpServletRequest = request instanceof HttpServletRequest;
        if (isHttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            String requestAsString = new Request(httpServletRequest, currentStackTrace()).toString();
            log.debug("Registered request: {}", requestAsString);
        } else {
            log.warn("Non-castable HTTP request. No registration will be performed");
        }
    }

    @SuppressWarnings({"squid:S1166", "ThrowCaughtLocally", "PMD.ExceptionAsFlowControl"})
    private StackTraceElement[] currentStackTrace() {
        try {
            throw new StackTraceMonitorException();
        } catch (StackTraceMonitorException exception) {
            return exception.getStackTrace();
        }
    }

    @Override
    public void doFilter(
        ServletRequest request, ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        boolean isEnabledUnwrapped = isEnabled.get();
        log.trace("Is filter enabled and the request will be registered? Answer: {}", isEnabledUnwrapped);
        if (isEnabledUnwrapped) {
            register(request);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.info("Destroyed {}", this);
    }
}
