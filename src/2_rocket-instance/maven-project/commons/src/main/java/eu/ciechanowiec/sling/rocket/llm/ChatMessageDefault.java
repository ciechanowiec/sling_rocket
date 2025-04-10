package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of a {@link ChatMessage}.
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
public class ChatMessageDefault implements ChatMessage {

    private final Supplier<Role> roleSupplier;
    private final Supplier<String> contentSupplier;

    /**
     * Constructs an instance of this class.
     *
     * @param role    type of the author of this {@link ChatMessage}
     * @param content content of this {@link ChatMessage}
     */
    @JsonCreator
    public ChatMessageDefault(
        @JsonProperty("role")
        Role role,
        @JsonProperty("content")
        String content
    ) {
        this.roleSupplier = () -> role;
        this.contentSupplier = () -> content;
        log.trace("Initialized {} with content: '{}'. Role: {}", this, content, role);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param roleSupplier    {@link Supplier} that produces the type of the author of this {@link ChatMessage}
     * @param contentSupplier {@link Supplier} that produces the content of this {@link ChatMessage}
     */
    public ChatMessageDefault(Supplier<Role> roleSupplier, Supplier<String> contentSupplier) {
        this.roleSupplier = roleSupplier;
        this.contentSupplier = contentSupplier;
        log.trace("Initialized {}", this);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param sourceMessage      {@link ChatMessage} to be used as a source of the {@link Role} and content for the
     *                           newly constructed {@link ChatMessage}
     * @param contentTransformer function that will be applied to the content of the {@code sourceMessage} and to
     *                           produce the content of the newly constructed {@link ChatMessage}
     */
    public ChatMessageDefault(ChatMessage sourceMessage, UnaryOperator<String> contentTransformer) {
        this.roleSupplier = sourceMessage::role;
        String initialContent = sourceMessage.content();
        this.contentSupplier = () -> contentTransformer.apply(initialContent);
        log.trace("Initialized {}. Initial content: '{}'", this, initialContent);
    }

    @Override
    @JsonProperty("content")
    public String content() {
        return contentSupplier.get();
    }

    @Override
    @JsonProperty("role")
    public Role role() {
        return roleSupplier.get();
    }
}
