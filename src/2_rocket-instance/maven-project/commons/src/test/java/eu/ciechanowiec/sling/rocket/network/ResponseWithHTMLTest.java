package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

class ResponseWithHTMLTest extends TestEnvironment {

    ResponseWithHTMLTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @Test
    void basicSend() {
        MockSlingJakartaHttpServletResponse slingResponse = new MockSlingJakartaHttpServletResponse();
        ResponseWithHTML response = new ResponseWithHTML(
            slingResponse, "<div>Hello, Universe</div>",
            HttpServletResponse.SC_OK
        );
        response.send();
        assertAll(
            () -> assertEquals("<div>Hello, Universe</div>", slingResponse.getOutputAsString()),
            () -> assertTrue(slingResponse.isCommitted()),
            () -> assertEquals(HttpServletResponse.SC_OK, slingResponse.getStatus()),
            () -> assertEquals(MediaType.TEXT_HTML, slingResponse.getContentType()),
            () -> assertThrows(AlreadySentException.class, response::send)
        );
    }

    @SneakyThrows
    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void dontSendIfAlreadySent() {
        MockSlingJakartaHttpServletResponse slingResponse = new MockSlingJakartaHttpServletResponse();
        ResponseWithHTML response = new ResponseWithHTML(
            slingResponse, "<div>Hello, Universe</div>",
            HttpServletResponse.SC_OK
        );
        response.send();
        try (PrintWriter responseWriter = slingResponse.getWriter()) {
            responseWriter.write("<div>Hello, Universe</div>");
            responseWriter.flush();
        }
        slingResponse.flushBuffer();
        assertAll(
            () -> assertEquals("<div>Hello, Universe</div>", slingResponse.getOutputAsString()),
            () -> assertTrue(slingResponse.isCommitted()),
            () -> assertThrows(AlreadySentException.class, response::send)
        );
    }
}
