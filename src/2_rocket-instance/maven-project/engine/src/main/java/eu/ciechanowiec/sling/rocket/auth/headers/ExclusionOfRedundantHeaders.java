package eu.ciechanowiec.sling.rocket.auth.headers;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.servlet.whiteboard.Preprocessor;

import java.io.IOException;

/**
 * Ensures that {@link ServletResponse}s do not contain the following redundant HTTP headers:
 * <ol>
 *     <li>the {@code WWW-Authenticate} header set by
 *     {@code org.apache.sling.auth.core.impl.HttpBasicAuthenticationHandler}</li>
 *     <li>the {@link org.apache.sling.auth.core.AuthConstants#X_REASON} header</li>
 *     <li>the {@link org.apache.sling.auth.core.AuthConstants#X_REASON_CODE} header</li>
 * </ol>
 */
@Component(
    service = {Preprocessor.class, ExclusionOfRedundantHeaders.class}
)
@SlingServletFilter(
    scope = SlingServletFilterScope.REQUEST
)
@Slf4j
public class ExclusionOfRedundantHeaders implements Preprocessor {

    /**
     * Constructs an instance of this class.
     */
    @Activate
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public ExclusionOfRedundantHeaders() {
        // required by javadoc
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("Initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpServletResponse) {
            ServletResponse wrappedResponse = new ResponseWithoutRedundantHeaders(httpServletResponse);
            chain.doFilter(request, wrappedResponse);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        log.info("Destroyed");
    }
}
