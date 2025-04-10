package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.jcr.DeletableResource;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.network.Request;
import eu.ciechanowiec.sling.rocket.network.Response;
import eu.ciechanowiec.sling.rocket.network.Status;
import eu.ciechanowiec.sling.rocket.privilege.RequiresPrivilege;
import java.util.List;
import java.util.Optional;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

/**
 * Servlet for handling DELETE requests to Assets API.
 */
@Component(
    service = {ServletDelete.class, Servlet.class},
    immediate = true
)
@SlingServletResourceTypes(
    methods = HttpConstants.METHOD_POST,
    resourceTypes = AssetsAPI.ASSETS_API_RESOURCE_TYPE,
    selectors = ServletDelete.SELECTOR
)
@Slf4j
@ServiceDescription("Servlet for handling DELETE requests to Assets API")
public class ServletDelete extends SlingAllMethodsServlet implements RequiresPrivilege {

    static final String SELECTOR = "delete";

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
    public ServletDelete(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess
    ) {
        this.fullResourceAccess = fullResourceAccess;
        log.info("Initialized {}", this);
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("PMD.CloseResource")
    protected void doPost(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        String userID = resourceResolver.getUserID();
        AuthIDUser authIDUser = new AuthIDUser(userID);
        UserResourceAccess userResourceAccess = new UserResourceAccess(authIDUser, fullResourceAccess);
        Request slingRequest = new Request(request, userResourceAccess);
        log.trace("Processing {}", slingRequest);
        RequestDelete requestDelete = new RequestDelete(slingRequest);
        if (requestDelete.isValidStructure()) {
            requestDelete.targetAsset()
                .map(asset -> new DeletableResource(asset, userResourceAccess))
                .flatMap(DeletableResource::delete)
                .map(
                    deletedPath -> new Response(
                        response, new Status(HttpServletResponse.SC_OK, "Asset deleted"),
                        List.of(new AssetDescriptor(requestDelete))
                    )
                ).or(
                    () -> Optional.of(
                        new Response(
                            response, new Status(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Unable to delete: '%s'".formatted(new AssetDescriptor(requestDelete))
                        )
                        ))
                ).ifPresent(Response::send);
        } else {
            Response responseWithError = new Response(
                response, new Status(HttpServletResponse.SC_BAD_REQUEST, "Invalid request structure")
            );
            responseWithError.send();
        }
    }

    @Override
    public List<String> requiredPrivileges() {
        return new DeletableResource(new TargetJCRPath("/"), fullResourceAccess).requiredPrivileges();
    }
}
