package eu.ciechanowiec.sling.rocket.asset.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.ws.rs.core.MediaType;
import java.util.Objects;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.TooManyStaticImports")
class ServletDefaultTest extends TestEnvironment {

    private ServletDefault servletDefault;

    ServletDefaultTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        context.registerInjectActivateService(AssetsAPI.class);
        context.registerInjectActivateService(AssetsAPIDocumentationDefault.class);
        servletDefault = context.registerInjectActivateService(ServletDefault.class);
    }

    @SuppressWarnings("VariableDeclarationUsageDistance")
    @SneakyThrows
    @Test
    void mustGetHTML() {
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDefault.doGet(request, response);
        assertAll(
            () -> assertEquals(HttpServletResponse.SC_OK, response.getStatus()),
            () -> assertTrue(response.getOutputAsString().contains("<!DOCTYPE html>")),
            () -> assertEquals(MediaType.TEXT_HTML, response.getContentType())
        );
    }
}
