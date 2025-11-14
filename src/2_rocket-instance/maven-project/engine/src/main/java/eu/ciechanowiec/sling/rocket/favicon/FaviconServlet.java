package eu.ciechanowiec.sling.rocket.favicon;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingJakartaSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet for handling favicon requests through {@link FaviconAPI}.
 */
@Component(
    service = {FaviconServlet.class, Servlet.class},
    immediate = true
)
@SlingServletResourceTypes(
    methods = HttpConstants.METHOD_GET,
    resourceTypes = FaviconAPI.FAVICON_RESOURCE_TYPE
)
@Slf4j
@ServiceDescription("Servlet for handling favicon requests through FaviconAPI")
@ToString
public class FaviconServlet extends SlingJakartaSafeMethodsServlet {

    /**
     * Constructs an instance of this class.
     */
    @Activate
    public FaviconServlet() {
        log.info("Initialized {}", this);
    }

    @Override
    @SuppressWarnings({"NullableProblems", "LineLength"})
    protected void doGet(
        SlingJakartaHttpServletRequest request,
        SlingJakartaHttpServletResponse response
    ) throws IOException {
        response.setContentType(ContentType.IMAGE_SVG.getMimeType());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><text y=\".9em\" font-size=\"90\">🚀</text></svg>"
        );
    }
}
