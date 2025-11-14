package eu.ciechanowiec.sling.rocket.llm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.SimpleNode;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

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
public class LLM implements WithJCRPath {

    private final AtomicReference<LLMConfigObfuscated> config;
    private final LLMStats llmStats;
    private final FullResourceAccess fullResourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param config             {@link LLMConfig} to configure this {@link LLM}
     * @param fullResourceAccess {@link FullResourceAccess} that will be used by the constructed object to acquire
     *                           access to resources
     */
    @Activate
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_INFERRED")
    public LLM(
        LLMConfig config,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        FullResourceAccess fullResourceAccess
    ) {
        this.config = new AtomicReference<>(new LLMConfigObfuscated(config));
        this.fullResourceAccess = fullResourceAccess;
        this.llmStats = new LLMStats(this, fullResourceAccess);
        new SimpleNode(
            this, fullResourceAccess, JcrResourceConstants.NT_SLING_ORDERED_FOLDER
        ).ensureNodeExists();
        log.info("Initialized {}", this);
    }

    /**
     * Configure this {@link LLM}.
     *
     * @param config {@link LLMConfig} to configure this {@link LLM}
     */
    @Modified
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_INFERRED")
    void configure(LLMConfig config) {
        log.info("Configuring {}", config);
        this.config.set(new LLMConfigObfuscated(config));
        new SimpleNode(
            this.config.get().jcrHome(), fullResourceAccess, JcrResourceConstants.NT_SLING_ORDERED_FOLDER
        ).ensureNodeExists();
        log.info("Configured {}", config);
    }

    /**
     * Generates a {@link ChatCompletion} for a given {@link Chat} out of the passed {@link ChatMessage}-s from that
     * {@link Chat}.
     *
     * @param chatMessages list of consecutive {@link ChatMessage}-s from a single {@link Chat} out of which a
     *                     {@link ChatCompletion} should be generated
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
            llmConfigObfuscated.llmAPIurl(),
            llmConfigObfuscated.llmAPIBearerToken(),
            chatCompletionRequestBody
        );
        ChatCompletion chatCompletion = chatCompletionRequest.execute();
        log.trace("{} completed chat for {} with {}", this, chatMessages, chatCompletion);
        llmStats.register(chatCompletion);
        return chatCompletion;
    }

    /**
     * Returns {@link LLMStats} of this {@link LLM}.
     *
     * @return {@link LLMStats} of this {@link LLM}
     */
    public LLMStats llmStats() {
        return llmStats;
    }

    /**
     * Same as {@link LLMConfig#llm_context$_$window_size()}.
     *
     * @return same as {@link LLMConfig#llm_context$_$window_size()}
     */
    public int contextWindowSize() {
        return config.get().contextWindowSize();
    }

    /**
     * Same as {@link LLMConfig#jcr_home()}.
     *
     * @return same as {@link LLMConfig#jcr_home()}
     */
    @Override
    public JCRPath jcrPath() {
        return config.get().jcrHome();
    }
}
