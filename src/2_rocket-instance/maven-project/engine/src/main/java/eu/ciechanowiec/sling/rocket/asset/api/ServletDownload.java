package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.network.*;
import eu.ciechanowiec.sling.rocket.privilege.RequiresPrivilege;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingJakartaSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.List;

/**
 * Servlet for handling DOWNLOAD requests to Assets API.
 */
@Component(
    service = {ServletDownload.class, Servlet.class},
    immediate = true
)
@SlingServletResourceTypes(
    methods = HttpConstants.METHOD_GET,
    resourceTypes = AssetsAPI.ASSETS_API_RESOURCE_TYPE,
    selectors = ServletDownload.SELECTOR
)
@Slf4j
@ServiceDescription("Servlet for handling DOWNLOAD requests to Assets API")
public class ServletDownload extends SlingJakartaSafeMethodsServlet implements RequiresPrivilege {

    static final String SELECTOR = "download";

    /**
     * {@link FullResourceAccess} that will be used by this {@link ServletDefault} to acquire access to resources.
     */
    private final FullResourceAccess fullResourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                           access to resources
     */
    @Activate
    public ServletDownload(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess
    ) {
        this.fullResourceAccess = fullResourceAccess;
        log.info("Initialized {}", this);
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    protected void doGet(
        @NotNull SlingJakartaHttpServletRequest request, @NotNull SlingJakartaHttpServletResponse response
    ) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        String userID = resourceResolver.getUserID();
        AuthIDUser authIDUser = new AuthIDUser(userID);
        UserResourceAccess userResourceAccess = new UserResourceAccess(authIDUser, fullResourceAccess);
        SlingRequest slingRequest = new SlingRequest(request, userResourceAccess);
        log.trace("Processing {}", slingRequest);
        RequestDownload requestDownload = new RequestDownload(slingRequest);
        if (requestDownload.isValidStructure()) {
            requestDownload.targetAsset()
                .map(asset -> new ResponseWithAsset(response, asset))
                .ifPresentOrElse(
                    responseWithAsset -> responseWithAsset.send(ContentDispositionHeader.ATTACHMENT),
                    () -> {
                        Response responseWithError = new Response(
                            response, new Status(
                            HttpServletResponse.SC_NOT_FOUND,
                            "No asset found: '%s'".formatted(new AssetDescriptor(requestDownload))
                        )
                        );
                        responseWithError.send();
                    }
                );
        } else {
            Response responseWithError = new Response(
                response, new Status(HttpServletResponse.SC_BAD_REQUEST, "Invalid request structure")
            );
            responseWithError.send();
        }
    }

    @Override
    public List<String> requiredPrivileges() {
        return List.of(PrivilegeConstants.JCR_READ);
    }
}
