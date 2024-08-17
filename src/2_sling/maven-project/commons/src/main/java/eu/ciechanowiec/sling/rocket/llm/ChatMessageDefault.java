package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Default implementation of {@link ChatMessage}.
 */
@ToString
@Slf4j
public class ChatMessageDefault implements ChatMessage {

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final Role role;
    @ToString.Exclude
    private final Supplier<String> contentSupplier;

    /**
     * Constructs an instance of this class.
     * @param role type of the author of this {@link ChatMessage}
     * @param content content of this {@link ChatMessage}
     */
    @JsonCreator
    public ChatMessageDefault(@JsonProperty("role") Role role, @JsonProperty("content") String content) {
        this.role = role;
        this.contentSupplier = () -> content;
        log.trace("Initialized {} with content: '{}'", this, content);
    }

    /**
     * Constructs an instance of this class.
     * @param sourceMessage {@link ChatMessage} to be used as a source of the {@link Role}
     *                      and content for the newly constructed {@link ChatMessage}
     * @param contentTransformer function that will be applied to the content of the {@code sourceMessage}
     *                           and to produce the content of the newly constructed {@link ChatMessage}
     */
    @SuppressWarnings("WeakerAccess")
    public ChatMessageDefault(ChatMessage sourceMessage, UnaryOperator<String> contentTransformer) {
        this.role = sourceMessage.role();
        String initialContent = sourceMessage.content();
        this.contentSupplier = () -> contentTransformer.apply(initialContent);
        log.trace("Initialized {}. Initial content: '{}'", this, initialContent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty("content")
    public String content() {
        return contentSupplier.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty("role")
    public Role role() {
        return role;
    }
}
