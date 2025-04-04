package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.network.Request;
import eu.ciechanowiec.sling.rocket.network.ResponseWithHTML;
import eu.ciechanowiec.sling.rocket.privilege.RequiresPrivilege;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Servlet for handling DEFAULT requests to Assets API.
 */
@Component(
    service = {ServletDefault.class, Servlet.class},
    immediate = true
)
@SlingServletResourceTypes(
    methods = HttpConstants.METHOD_GET,
    resourceTypes = AssetsAPI.ASSETS_API_RESOURCE_TYPE
)
@Slf4j
@ServiceDescription("Servlet for handling DEFAULT requests to Assets API")
@SuppressWarnings("PMD.ExcessiveImports")
public class ServletDefault extends SlingSafeMethodsServlet implements RequiresPrivilege {

    /**
     * {@link FullResourceAccess} that will be used by this {@link ServletDefault} to acquire access to resources.
     */
    private final FullResourceAccess fullResourceAccess;

    /**
     * HTML content to send in response to all requests that hit this {@link ServletDefault}.
     */
    private final MemoizingSupplier<String> htmlToSend;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess     {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                               access to resources
     * @param assetsAPIDocumentation {@link AssetsAPIDocumentation} that will be used to provide Assets API
     *                               documentation
     */
    @Activate
    public ServletDefault(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess,
        @Reference(
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY
        )
        AssetsAPIDocumentation assetsAPIDocumentation
    ) {
        this.fullResourceAccess = fullResourceAccess;
        this.htmlToSend = new MemoizingSupplier<>(assetsAPIDocumentation::html);
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        String userID = resourceResolver.getUserID();
        AuthIDUser authIDUser = new AuthIDUser(userID);
        UserResourceAccess userResourceAccess = new UserResourceAccess(authIDUser, fullResourceAccess);
        Request slingRequest = new Request(request, userResourceAccess);
        log.trace("Processing {}", slingRequest);
        ResponseWithHTML responseWithHTML = new ResponseWithHTML(
            response, htmlToSend.get(), HttpServletResponse.SC_OK
        );
        responseWithHTML.send();
    }

    @Override
    public List<String> requiredPrivileges() {
        return List.of();
    }
}
