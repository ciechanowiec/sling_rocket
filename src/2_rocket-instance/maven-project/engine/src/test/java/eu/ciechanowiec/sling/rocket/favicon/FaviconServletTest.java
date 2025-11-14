package eu.ciechanowiec.sling.rocket.favicon;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FaviconServletTest extends TestEnvironment {

    private FaviconServlet faviconServlet;

    FaviconServletTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        context.registerInjectActivateService(FaviconAPI.class);
        faviconServlet = context.registerInjectActivateService(FaviconServlet.class);
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("LineLength")
    void mustGetSVG() {
        context.currentResource(FaviconAPI.FAVICON_PATH);
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        MockSlingJakartaHttpServletResponse response = context.jakartaResponse();

        faviconServlet.doGet(request, response);

        String expectedSvg
            = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><text y=\".9em\" font-size=\"90\">🚀</text></svg>";
        assertAll(
            () -> assertEquals(HttpServletResponse.SC_OK, response.getStatus()),
            () -> assertEquals("image/svg+xml;charset=UTF-8", response.getContentType()),
            () -> assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding()),
            () -> assertEquals(expectedSvg, response.getOutputAsString())
        );
    }
}
