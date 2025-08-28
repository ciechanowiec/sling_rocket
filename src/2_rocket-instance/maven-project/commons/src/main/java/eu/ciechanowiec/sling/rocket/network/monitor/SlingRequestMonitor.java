package eu.ciechanowiec.sling.rocket.network.monitor;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.network.SlingRequest;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.metatype.annotations.Designate;

import javax.servlet.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Logs all information about the incoming HTTP requests on the Sling level. Used for debugging purposes
 */
@Component(
    immediate = true,
    service = {Filter.class, SlingRequestMonitor.class},
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@SlingServletFilter(
    pattern = "/.*",
    scope = SlingServletFilterScope.REQUEST
)
@ServiceDescription(
    "Logs all information about the incoming HTTP requests on the Sling level. Used for debugging purposes"
)
@Designate(
    ocd = SlingRequestMonitorConfig.class
)
@ServiceRanking(0)
@Slf4j
@ToString
public class SlingRequestMonitor implements Filter {

    private final FullResourceAccess fullResourceAccess;
    private final AtomicBoolean isEnabled;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                           access to resources
     * @param config             {@link SlingRequestMonitorConfig} that will be used by the constructed object
     */
    @Activate
    public SlingRequestMonitor(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess,
        SlingRequestMonitorConfig config
    ) {
        this.fullResourceAccess = fullResourceAccess;
        this.isEnabled = new AtomicBoolean(config.is$_$enabled());
        log.debug("Initialized {}", this);
    }

    @Modified
    void configure(SlingRequestMonitorConfig config) {
        isEnabled.set(config.is$_$enabled());
        log.debug("Reconfigured {}", this);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("Initiated {}", this);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        boolean isEnabledUnwrapped = isEnabled.get();
        log.trace("Is filter enabled and the request will be registered? Answer: {}", isEnabledUnwrapped);
        if (isEnabledUnwrapped) {
            register(request);
        }
        chain.doFilter(request, response);
    }

    @SuppressWarnings("PMD.CloseResource")
    private void register(ServletRequest request) {
        boolean isSlingRequest = request instanceof SlingHttpServletRequest;
        if (isSlingRequest) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            ResourceResolver resourceResolver = slingRequest.getResourceResolver();
            String userID = resourceResolver.getUserID();
            AuthIDUser authIDUser = new AuthIDUser(userID);
            UserResourceAccess userResourceAccess = new UserResourceAccess(authIDUser, fullResourceAccess);
            String requestAsString = new SlingRequest(
                slingRequest, currentStackTrace(), userResourceAccess
            ).toString();
            log.debug(
                "Registered request: {}", requestAsString
            );
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
    public void destroy() {
        log.info("Destroyed {}", this);
    }
}
