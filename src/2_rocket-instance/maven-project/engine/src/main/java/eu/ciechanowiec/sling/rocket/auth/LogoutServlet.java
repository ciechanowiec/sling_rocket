package eu.ciechanowiec.sling.rocket.auth;

import jakarta.servlet.Servlet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

/**
 * Servlet for handling user logouts through {@link LogoutAPI}.
 */
@Component(
    service = {LogoutServlet.class, Servlet.class},
    immediate = true
)
@SlingServletResourceTypes(
    methods = HttpConstants.METHOD_POST,
    resourceTypes = LogoutAPI.LOGOUT_API_RESOURCE_TYPE
)
@Slf4j
@ServiceDescription("Servlet for handling user logouts through LogoutAPI")
public class LogoutServlet extends SlingJakartaAllMethodsServlet {

    /**
     * {@link Authenticator} that will handle the logout request.
     */
    private final Authenticator authenticator;

    /**
     * Constructs an instance of this class.
     *
     * @param authenticator {@link Authenticator} that will be used by the constructed object to acquire access to
     *                      resources
     */
    @Activate
    public LogoutServlet(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        Authenticator authenticator
    ) {
        this.authenticator = authenticator;
    }

    @Override
    protected void doPost(
        @NotNull SlingJakartaHttpServletRequest request,
        @NotNull SlingJakartaHttpServletResponse response
    ) {
        String remoteUser = request.getRemoteUser();
        log.debug("Logging out user: '{}'", remoteUser);
        AuthUtil.setLoginResourceAttribute(request, StringUtils.EMPTY);
        authenticator.logout(request, response);
    }
}
