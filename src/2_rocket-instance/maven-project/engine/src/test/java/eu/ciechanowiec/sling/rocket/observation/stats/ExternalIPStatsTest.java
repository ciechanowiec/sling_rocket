package eu.ciechanowiec.sling.rocket.observation.stats;

import com.sun.net.httpserver.HttpServer;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidUsingHardCodedIP", "PMD.AvoidDuplicateLiterals"})
class ExternalIPStatsTest extends TestEnvironment {

    private HttpServer server;
    private int port;

    ExternalIPStatsTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
            "/ip", exchange -> {
                String response = "1.2.3.4";
                exchange.sendResponseHeaders(HttpServletResponse.SC_OK, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(Charset.defaultCharset()));
                }
            }
        );
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void testExternalIPStats() {
        URI uri = URI.create("http://localhost:" + port + "/ip");
        ExternalIPStats externalIPStats = new ExternalIPStats(List.of(uri));

        assertEquals(ExternalIPStats.class.getName(), externalIPStats.name());

        String json = externalIPStats.asJSON();
        assertTrue(json.contains("1.2.3.4"));
        assertTrue(json.contains("externalIP"));
    }

    @Test
    void testFallback() {
        URI failingUri = URI.create("http://localhost:" + port + "/fail");
        URI succeedingUri = URI.create("http://localhost:" + port + "/ip");

        server.createContext(
            "/fail", exchange -> {
                exchange.sendResponseHeaders(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
                exchange.close();
            }
        );

        JSON externalIPStats = new ExternalIPStats(List.of(failingUri, succeedingUri));
        String json = externalIPStats.asJSON();

        assertTrue(json.contains("1.2.3.4"));
        assertTrue(json.contains("externalIP"));
    }

    @Test
    void testFirstResponds() {
        URI firstUri = URI.create("http://localhost:" + port + "/ip1");
        URI secondUri = URI.create("http://localhost:" + port + "/ip2");

        server.createContext(
            "/ip1", exchange -> {
                String response = "1.1.1.1";
                exchange.sendResponseHeaders(HttpServletResponse.SC_OK, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(Charset.defaultCharset()));
                }
            }
        );
        server.createContext(
            "/ip2", exchange -> {
                String response = "2.2.2.2";
                exchange.sendResponseHeaders(HttpServletResponse.SC_OK, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(Charset.defaultCharset()));
                }
            }
        );

        JSON externalIPStats = new ExternalIPStats(List.of(firstUri, secondUri));
        String json = externalIPStats.asJSON();

        assertTrue(json.contains("1.1.1.1"));
        assertFalse(json.contains("2.2.2.2"));
    }

    @Test
    void testAllFail() {
        URI failingUri1 = URI.create("http://localhost:" + port + "/fail1");
        URI failingUri2 = URI.create("http://localhost:" + port + "/fail2");

        server.createContext(
            "/fail1", exchange -> {
                exchange.sendResponseHeaders(HttpServletResponse.SC_NOT_FOUND, 0);
                exchange.close();
            }
        );
        server.createContext(
            "/fail2", exchange -> {
                exchange.sendResponseHeaders(HttpServletResponse.SC_SERVICE_UNAVAILABLE, 0);
                exchange.close();
            }
        );

        JSON externalIPStats = new ExternalIPStats(List.of(failingUri1, failingUri2));
        String json = externalIPStats.asJSON();

        assertFalse(json.contains("externalIP"), "Should not contain externalIP if all services fail: " + json);
    }

    @Test
    void testEmptyResponse() {
        URI emptyUri = URI.create("http://localhost:" + port + "/empty");

        server.createContext(
            "/empty", exchange -> {
                exchange.sendResponseHeaders(HttpServletResponse.SC_OK, 0);
                exchange.close();
            }
        );

        JSON externalIPStats = new ExternalIPStats(List.of(emptyUri));
        String json = externalIPStats.asJSON();

        // It will contain externalIP: "" if Jackson handles Optional.of("") this way
        assertTrue(json.contains("externalIP"), "Should contain externalIP if response was 200 OK: " + json);
        assertTrue(json.contains("\"\""));
    }
}
