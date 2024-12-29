package eu.ciechanowiec.sling.rocket.llm;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Large language model.
 */
@Component(
        service = LLM.class,
        immediate = true
)
@Slf4j
@ServiceDescription("Large language model")
@Designate(ocd = LLMConfig.class, factory = true)
@SuppressWarnings({"unused", "WeakerAccess"})
@ToString
public class LLM {

    private final AtomicReference<LLMConfigObfuscated> config;

    /**
     * Constructs an instance of this class.
     * @param config {@link LLMConfig} to configure this {@link LLM}
     */
    @Activate
    public LLM(LLMConfig config) {
        this.config = new AtomicReference<>(new LLMConfigObfuscated(config));
        log.info("Initialized {}", this);
    }

    /**
     * Configure this {@link LLM}.
     * @param config {@link LLMConfig} to configure this {@link LLM}
     */
    @Modified
    void configure(LLMConfig config) {
        log.info("Configuring {}", config);
        this.config.set(new LLMConfigObfuscated(config));
        log.info("Configured {}", config);
    }

    /**
     * Generates a {@link ChatCompletion} for a given {@link Chat} out of the passed {@link ChatMessage}-s
     * from that {@link Chat}.
     *
     * @param chatMessages list of consecutive {@link ChatMessage}-s from a single {@link Chat}
     *                     out of which a {@link ChatCompletion} should be generated
     * @return {@link ChatCompletion} generated out of the passed {@link ChatMessage}-s
     */
    public ChatCompletion complete(List<ChatMessage> chatMessages) {
        log.trace("{} is completing chat for {}", this, chatMessages);
        LLMConfigObfuscated llmConfigObfuscated = config.get();
        ChatCompletionRequestBody chatCompletionRequestBody = new CCRequestBodyDefault(
                llmConfigObfuscated.llmModel(),
                chatMessages,
                llmConfigObfuscated::llmMaxTokens,
                llmConfigObfuscated::llmMaxCompletionTokens,
                () -> Optional.of(llmConfigObfuscated.llmTemperature()),
                () -> Optional.of(llmConfigObfuscated.llmFrequencyPenalty()),
                () -> Optional.of(llmConfigObfuscated.llmTopP())
        );
        ChatCompletionRequest chatCompletionRequest = new CCRequestDefault(
                URI.create(llmConfigObfuscated.llmAPIurl()),
                llmConfigObfuscated.llmAPIBearerToken(),
                chatCompletionRequestBody
        );
        ChatCompletion chatCompletion = chatCompletionRequest.execute();
        log.trace("{} completed chat for {} with {}", this, chatMessages, chatCompletion);
        return chatCompletion;
    }
}
