package eu.ciechanowiec.sling.rocket.asset.api;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.Assets;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.network.Affected;
import eu.ciechanowiec.sling.rocket.network.Request;
import eu.ciechanowiec.sling.rocket.network.Response;
import eu.ciechanowiec.sling.rocket.network.Status;
import eu.ciechanowiec.sling.rocket.privilege.RequiresPrivilege;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.engine.impl.parameters.RequestParameterSupportConfigurer;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import javax.servlet.Servlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Servlet for handling UPLOAD requests to Assets API.
 * <p>
 * Upload limitations can be set via configuring {@link RequestParameterSupportConfigurer#PID}.
 */
@Component(
        service = {ServletUpload.class, Servlet.class},
        immediate = true,
        configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@Designate(ocd = ServletUploadConfig.class)
@SlingServletResourceTypes(
        methods = HttpConstants.METHOD_POST,
        resourceTypes = AssetsAPI.ASSETS_API_RESOURCE_TYPE,
        extensions = ServletUpload.EXTENSION
)
@Slf4j
@ServiceDescription("Servlet for handling UPLOAD requests to Assets API")
@SuppressWarnings({"PMD.ExcessiveImports", "JavadocReference"})
@SuppressFBWarnings({"MSF_MUTABLE_SERVLET_FIELD", "MTIA_SUSPECT_SERVLET_INSTANCE_FIELD"})
@MultipartConfig
public class ServletUpload extends SlingAllMethodsServlet implements RequiresPrivilege {

    static final String EXTENSION = "upload";

    /**
     * {@link FullResourceAccess} that will be used by this {@link ServletDefault} to acquire access to resources.
     */
    private final FullResourceAccess fullResourceAccess;

    /**
     * {@link ServletUploadConfig} for this {@link ServletUpload}.
     */
    private ServletUploadConfig config;

    /**
     * {@link DownloadLink} that will be used by this {@link ServletUpload} to generate download links for
     * uploaded {@link Asset}s.
     */
    private final DownloadLink downloadLink;

    /**
     * Constructs an instance of this class.
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed
     *                           object to acquire access to resources
     * @param config {@link ServletUploadConfig} that will be used by the constructed object
     * @param downloadLink {@link DownloadLink} that will be used by this {@link ServletUpload}
     *                     to generate download links for uploaded {@link Asset}s
     */
    @Activate
    public ServletUpload(
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            FullResourceAccess fullResourceAccess,
            ServletUploadConfig config,
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            DownloadLink downloadLink
    ) {
        this.fullResourceAccess = fullResourceAccess;
        this.config = config;
        this.downloadLink = downloadLink;
        ensurePath(fullResourceAccess, new TargetJCRPath(this.config.jcr_path()));
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(ServletUploadConfig config) {
        this.config = config;
        ensurePath(fullResourceAccess, new TargetJCRPath(this.config.jcr_path()));
        log.info("Configured {}", this);
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    protected void doPost(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        String userID = resourceResolver.getUserID();
        AuthIDUser authIDUser = new AuthIDUser(userID);
        UserResourceAccess userResourceAccess = new UserResourceAccess(authIDUser, fullResourceAccess);
        Request slingRequest = new Request(request, userResourceAccess);
        log.trace("Processing {}", slingRequest);
        RequestUpload requestUpload = new RequestUpload(slingRequest, downloadLink);
        if (requestUpload.isValidStructure()) {
            List<Affected> savedAssets = requestUpload.saveAssets(
                    new ParentJCRPath(new TargetJCRPath(config.jcr_path())), config.do$_$include$_$download$_$link()
            );
            Status status = Conditional.conditional(savedAssets.isEmpty())
                    .onTrue(() -> new Status(HttpServletResponse.SC_BAD_REQUEST, "No files uploaded"))
                    .onFalse(() -> new Status(HttpServletResponse.SC_CREATED, "File(s) uploaded"))
                    .get(Status.class);
            Response slingResponse = new Response(response, status, savedAssets);
            slingResponse.send();
        } else {
            Response responseWithError = new Response(
                    response, new Status(HttpServletResponse.SC_BAD_REQUEST, "Invalid request structure")
            );
            responseWithError.send();
        }
    }

    @SneakyThrows
    @SuppressWarnings("TypeMayBeWeakened")
    private void ensurePath(FullResourceAccess fullResourceAccess, JCRPath pathToEnsure) {
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
            String pathToEnsureRaw = pathToEnsure.get();
            Resource resource = ResourceUtil.getOrCreateResource(
                    resourceResolver, pathToEnsureRaw,
                    Map.of(JcrConstants.JCR_PRIMARYTYPE, Assets.NT_ASSETS), null, true
            );
            log.info("Ensured {}", resource);
        }
    }

    @Override
    public List<String> requiredPrivileges() {
        return List.of(
                PrivilegeConstants.JCR_READ, PrivilegeConstants.JCR_ADD_CHILD_NODES,
                PrivilegeConstants.JCR_MODIFY_PROPERTIES, PrivilegeConstants.JCR_NODE_TYPE_MANAGEMENT
        );
    }
}
