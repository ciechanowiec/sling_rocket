package eu.ciechanowiec.sling.rocket.llm;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class LLMTest extends TestEnvironment {

    private Server server;
    private LLM llm;

    LLMTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @SneakyThrows
    @BeforeEach
    @SuppressWarnings({"MagicNumber", "PMD.CloseResource"})
    void setup() {
        server = new Server(0);
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(new ServletHolder(new LLMAPI()), "/*");
        server.setHandler(servletHandler);
        server.start();
        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        int port = connector.getLocalPort();
        log.info("Jetty Server started on dynamic port: {}", port);
        llm = context.registerInjectActivateService(LLM.class, Map.of(
                "llm.api.url", "http://localhost:%d".formatted(port),
                "llm.api.bearer.token", "this-is-a-secret-test-token",
                "llm.model", "gpt-839104284",
                "llm.max_tokens", 0,
                "llm.max_completion_tokens", 320,
                "llm.frequency_penalty", 0.8,
                "llm.temperature", 0.8,
                "llm.top_p", 0.8,
                "jcr.home", "/content/rocket/llm/openai"
        ));
    }

    @AfterEach
    void teardown() {
        Optional.ofNullable(server)
                .filter(Server::isRunning)
                .ifPresent(
                        SneakyConsumer.sneaky(
                                nonNullServer -> {
                                    nonNullServer.stop();
                                    log.info("Jetty Server stopped");
                                }
                        )
                );
    }

    @Test
    @SuppressWarnings({"LineLength", "MagicNumber"})
    void testCompletion() {
        List<ChatMessage> messages = List.of(
                new ChatMessageDefault(Role.SYSTEM, "Answer like you are HAL 9000."),
                new ChatMessageDefault(Role.USER, "What is your name?")
        );
        ChatCompletion chatCompletion = llm.complete(messages);
        String chatCompletionJSON = chatCompletion.asJSON();
        assertAll(
                () -> assertEquals(48, llm.llmStats().numOfGeneratedCharacters()),
                () -> assertEquals(16, llm.llmStats().numOfGeneratedTokens())
        );
        assertEquals(
                "{\"id\":\"chatcmpl-AjpkS3315YWRC4AZJJ3gEYbqRISDx\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"My name is HAL 9000. How can I assist you today?\"}}],\"created\":\"2024-12-29T15:44:04\",\"model\":\"gpt-4o-2024-08-06\",\"usage\":{\"prompt_tokens\":25,\"completion_tokens\":16,\"total_tokens\":41}}",
                chatCompletionJSON
        );
        llm.complete(messages);
        assertAll(
                () -> assertEquals(48 * 2, llm.llmStats().numOfGeneratedCharacters()),
                () -> assertEquals(16 * 2, llm.llmStats().numOfGeneratedTokens())
        );
    }

    private static final class LLMAPI extends HttpServlet {

        @SneakyThrows
        @Override
        @SuppressWarnings({"MethodLength", "LineLength"})
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
            assertEquals(MediaType.APPLICATION_JSON, req.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(
                    "Bearer %s".formatted("this-is-a-secret-test-token"), req.getHeader(HttpHeaders.AUTHORIZATION)
            );
            String requestBody = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining());
            assertEquals(
                    "{\"model\":\"gpt-839104284\",\"messages\":[{\"role\":\"system\",\"content\":\"Answer like you are HAL 9000.\"},{\"role\":\"user\",\"content\":\"What is your name?\"}],\"max_completion_tokens\":320,\"temperature\":0.8,\"frequency_penalty\":0.8,\"top_p\":0.8}",
                    requestBody
            );
            String responseBody = """
                    {
                      "id": "chatcmpl-AjpkS3315YWRC4AZJJ3gEYbqRISDx",
                      "object": "chat.completion",
                      "created": 1735487044,
                      "model": "gpt-4o-2024-08-06",
                      "choices": [
                        {
                          "index": 0,
                          "message": {
                            "role": "assistant",
                            "content": "My name is HAL 9000. How can I assist you today?",
                            "refusal": null
                          },
                          "logprobs": null,
                          "finish_reason": "stop"
                        }
                      ],
                      "usage": {
                        "prompt_tokens": 25,
                        "completion_tokens": 16,
                        "total_tokens": 41,
                        "prompt_tokens_details": {
                          "cached_tokens": 0,
                          "audio_tokens": 0
                        },
                        "completion_tokens_details": {
                          "reasoning_tokens": 0,
                          "audio_tokens": 0,
                          "accepted_prediction_tokens": 0,
                          "rejected_prediction_tokens": 0
                        }
                      },
                      "system_fingerprint": "fp_d28bcae782"
                    }
                    """;
            resp.setContentType(MediaType.APPLICATION_JSON);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(responseBody);
        }
    }
}
