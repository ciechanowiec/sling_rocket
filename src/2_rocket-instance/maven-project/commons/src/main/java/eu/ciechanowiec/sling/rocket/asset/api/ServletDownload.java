package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.network.*;
import eu.ciechanowiec.sling.rocket.privilege.RequiresPrivilege;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
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
public class ServletDownload extends SlingSafeMethodsServlet implements RequiresPrivilege {

    static final String SELECTOR = "download";

    /**
     * {@link FullResourceAccess} that will be used by this {@link ServletDefault} to acquire access to resources.
     */
    private final FullResourceAccess fullResourceAccess;

    /**
     * Constructs an instance of this class.
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed
     *                           object to acquire access to resources
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
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        String userID = resourceResolver.getUserID();
        AuthIDUser authIDUser = new AuthIDUser(userID);
        UserResourceAccess userResourceAccess = new UserResourceAccess(authIDUser, fullResourceAccess);
        Request slingRequest = new Request(request, userResourceAccess);
        log.trace("Processing {}", slingRequest);
        RequestDownload requestDelete = new RequestDownload(slingRequest);
        if (requestDelete.isValidStructure()) {
            requestDelete.targetAsset()
                    .map(asset -> new ResponseWithAsset(response, asset))
                    .ifPresentOrElse(
                            responseWithAsset -> responseWithAsset.send(ContentDispositionHeader.ATTACHMENT),
                            () -> {
                                Response responseWithError = new Response(
                                        response, new Status(
                                        HttpServletResponse.SC_NOT_FOUND,
                                        "No asset found: '%s'".formatted(new AssetDescriptor(requestDelete))
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
