package eu.ciechanowiec.sling.rocket.llm;

import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.SimpleNode;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Statistics of {@link LLM}.
 */
@Slf4j
public class LLMStats {

    private static final String PN_NUM_OF_GENERATED_TOKENS = "numOfGeneratedTokens";
    private static final String PN_NUM_OF_GENERATED_CHARACTERS = "numOfGeneratedCharacters";

    private final JCRPath jcrPath;
    private final FullResourceAccess fullResourceAccess;
    private final Lock lock;

    LLMStats(LLM llm, FullResourceAccess fullResourceAccess) {
        this.jcrPath = new TargetJCRPath(new ParentJCRPath(llm), "stats");
        this.fullResourceAccess = fullResourceAccess;
        this.lock = new ReentrantLock();
    }

    /**
     * Total number of {@link Usage#completionTokens()} generated by the associated {@link LLM}.
     *
     * @return total number of {@link Usage#completionTokens()} generated by the associated {@link LLM}
     */
    @SuppressWarnings("WeakerAccess")
    public long numOfGeneratedTokens() {
        return new SimpleNode(jcrPath, fullResourceAccess).nodeProperties().propertyValue(
            PN_NUM_OF_GENERATED_TOKENS, DefaultProperties.LONG_CLASS
        ).orElse(NumberUtils.LONG_ZERO);
    }

    /**
     * Total number of {@link ChatMessage#content()} characters in all {@link Choice#message()}-s from all
     * {@link ChatCompletion#choices()} generated by the associated {@link LLM}.
     *
     * @return total number of {@link ChatMessage#content()} characters in all {@link Choice#message()}-s from all
     * {@link ChatCompletion#choices()} generated by the associated {@link LLM}
     */
    @SuppressWarnings("WeakerAccess")
    public long numOfGeneratedCharacters() {
        return new SimpleNode(jcrPath, fullResourceAccess).nodeProperties().propertyValue(
            PN_NUM_OF_GENERATED_CHARACTERS, DefaultProperties.LONG_CLASS
        ).orElse(NumberUtils.LONG_ZERO);
    }

    void register(ChatCompletion chatCompletion) {
        int numOfGeneratedTokens = chatCompletion.usage().completionTokens();
        int numOfGeneratedCharacters = chatCompletion.choices()
            .stream()
            .map(Choice::message)
            .map(ChatMessage::content)
            .collect(Collectors.joining())
            .length();
        register(numOfGeneratedTokens, numOfGeneratedCharacters);
    }

    private void register(int numOfGeneratedTokens, int numOfGeneratedCharacters) {
        lock.lock();
        try {
            log.trace(
                "Registering {} tokens and {} characters at {}",
                numOfGeneratedTokens, numOfGeneratedCharacters, jcrPath
            );
            NodeProperties nodeProperties = new SimpleNode(jcrPath, fullResourceAccess).nodeProperties();
            long currentNumOfGeneratedTokens = nodeProperties.propertyValue(
                PN_NUM_OF_GENERATED_TOKENS, DefaultProperties.LONG_CLASS
            ).orElse(NumberUtils.LONG_ZERO);
            @SuppressWarnings("PMD.LongVariable")
            long currentNumOfGeneratedCharacters = nodeProperties.propertyValue(
                PN_NUM_OF_GENERATED_CHARACTERS, DefaultProperties.LONG_CLASS
            ).orElse(NumberUtils.LONG_ZERO);
            long newNumOfGeneratedTokens = currentNumOfGeneratedTokens + numOfGeneratedTokens;
            long newNumOfGeneratedCharacters = currentNumOfGeneratedCharacters + numOfGeneratedCharacters;
            Map<String, Object> properties = Map.of(
                PN_NUM_OF_GENERATED_TOKENS, newNumOfGeneratedTokens,
                PN_NUM_OF_GENERATED_CHARACTERS, newNumOfGeneratedCharacters
            );
            nodeProperties.setProperties(properties).ifPresentOrElse(
                registeredNodeProperties -> log.info(
                    "Registered {} tokens and {} characters at {}",
                    numOfGeneratedTokens, numOfGeneratedCharacters, jcrPath
                ),
                () -> log.error(
                    "Failed to register {} tokens and {} characters at {}",
                    numOfGeneratedTokens, numOfGeneratedCharacters, jcrPath
                )
            );
        } finally {
            lock.unlock();
        }
    }
}
