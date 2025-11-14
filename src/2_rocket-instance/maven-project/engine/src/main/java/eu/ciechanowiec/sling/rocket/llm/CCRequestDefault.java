package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Default implementation of {@link ChatCompletionRequest}.
 */
@SuppressWarnings("WeakerAccess")
@ToString
@Slf4j
public class CCRequestDefault implements ChatCompletionRequest {

    private final URI llmAPIuri;
    @ToString.Exclude
    private final String llmAPIBearerToken;
    private final ChatCompletionRequestBody chatCompletionRequestBody;

    /**
     * Constructs an instance of this class.
     *
     * @param llmAPIuri                 {@link LLMConfig#llm_api_url()}
     * @param llmAPIBearerToken         {@link LLMConfig#llm_api_bearer_token()}
     * @param chatCompletionRequestBody {@link ChatCompletionRequestBody} of this {@link ChatCompletionRequest}
     */
    public CCRequestDefault(
        URI llmAPIuri, String llmAPIBearerToken, ChatCompletionRequestBody chatCompletionRequestBody
    ) {
        this.llmAPIuri = llmAPIuri;
        this.llmAPIBearerToken = llmAPIBearerToken;
        this.chatCompletionRequestBody = chatCompletionRequestBody;
        log.trace("Initialized {}", this);
    }

    @SneakyThrows
    @Override
    public ChatCompletion execute() {
        String requestBodyJSON = chatCompletionRequestBody.asJSON();
        log.trace("Executing {}. JSON body: '{}'", this, requestBodyJSON);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(llmAPIuri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(llmAPIBearerToken))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyJSON))
            .build();
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            String responseBodyJSON = response.body();
            log.trace(
                "Executed {} with this body: '{}'. Response JSON body: '{}'",
                this, requestBodyJSON, responseBodyJSON
            );
            return objectMapper.readValue(responseBodyJSON, ChatCompletion.class);
        }
    }
}
