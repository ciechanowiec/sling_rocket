package eu.ciechanowiec.sling.rocket.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.ws.rs.core.MediaType;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest extends TestEnvironment {

    ResponseTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @Test
    void basicSend() {
        MockSlingHttpServletResponse slingResponse = new MockSlingHttpServletResponse();
        Response response = new Response(
                slingResponse, new Status(HttpServletResponse.SC_BAD_REQUEST, "Invalid request structure"),
                List.of(
                        new AffectedResource("/content/rocket", "tres"), new AffectedResource("/apps/rocket", "uno")
                )
        );
        response.send();
        assertAll(
                () -> assertEquals(response.asJSON(), slingResponse.getOutputAsString()),
                () -> assertTrue(slingResponse.isCommitted()),
                () -> assertEquals(HttpServletResponse.SC_BAD_REQUEST, slingResponse.getStatus()),
                () -> assertEquals(MediaType.APPLICATION_JSON, slingResponse.getContentType()),
                () -> assertThrows(AlreadySentException.class, response::send)
        );
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void dontSendIfAlreadySent() {
        MockSlingHttpServletResponse slingResponse = new MockSlingHttpServletResponse();
        Response response = new Response(
                slingResponse, new Status(HttpServletResponse.SC_BAD_REQUEST, "Invalid request structure"),
                List.of(
                        new AffectedResource("/content/rocket", "tres"), new AffectedResource("/apps/rocket", "uno")
                )
        );
        try (PrintWriter responseWriter = slingResponse.getWriter()) {
            responseWriter.write("Some content");
            responseWriter.flush();
        }
        slingResponse.flushBuffer();
        assertAll(
                () -> assertEquals("Some content", slingResponse.getOutputAsString()),
                () -> assertTrue(slingResponse.isCommitted()),
                () -> assertThrows(AlreadySentException.class, response::send)
        );
    }

    @SuppressWarnings("unused")
        private record AffectedResource(@JsonProperty("path") String path,
                                        @JsonProperty("id") String id) implements Affected {

            private AffectedResource(String path, String id) {
                this.path = path;
                this.id = id;
            }
        }
}
