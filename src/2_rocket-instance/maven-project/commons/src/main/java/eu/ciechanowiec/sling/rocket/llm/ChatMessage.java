package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents a single message in a conversation with an AI-based chatbot.
 */
@SuppressWarnings("ClassReferencesSubclass")
@JsonDeserialize(as = ChatMessageDefault.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ChatMessage {

    /**
     * Returns the {@link Role} of the author of this {@link ChatMessage}.
     * @return {@link Role} of the author of this {@link ChatMessage}
     */
    Role role();

    /**
     * Returns the content of this {@link ChatMessage}.
     * @return content of this {@link ChatMessage}
     */
    String content();
}
