package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.ws.rs.core.MediaType;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

class ResponseWithHTMLTest extends TestEnvironment {

    ResponseWithHTMLTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @Test
    void basicSend() {
        MockSlingHttpServletResponse slingResponse = new MockSlingHttpServletResponse();
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

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void dontSendIfAlreadySent() {
        MockSlingHttpServletResponse slingResponse = new MockSlingHttpServletResponse();
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