package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("MultipleStringLiterals")
class ChatMessageDefaultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SneakyThrows
    void serializeChatMessageDefaultToJson() {
        ChatMessage message = new ChatMessageDefault(Role.USER, "Hello, world!");
        String json = objectMapper.writeValueAsString(message);
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"content\":\"Hello, world!\""));
    }

    @Test
    @SneakyThrows
    void deserializeJsonToChatMessageDefault() {
        String json = "{\"role\":\"user\",\"content\":\"Hello, world!\"}";
        ChatMessage message = objectMapper.readValue(json, ChatMessageDefault.class);
        assertEquals(Role.USER, message.role());
        assertEquals("Hello, world!", message.content());
    }

    @Test
    @SneakyThrows
    void deserializeJsonToChatMessageInterface() {
        String json = "{\"role\":\"user\",\"content\":\"Hello, world!\"}";
        ChatMessage message = objectMapper.readValue(json, ChatMessage.class);
        assertEquals(Role.USER, message.role());
        assertEquals("Hello, world!", message.content());
    }

    @Test
    @SneakyThrows
    void serializeAndDeserializeChatMessageDefault() {
        ChatMessage originalMessage = new ChatMessageDefault(Role.ASSISTANT, "How can I help you?");
        String json = objectMapper.writeValueAsString(originalMessage);
        ChatMessageDefault deserializedMessage = objectMapper.readValue(json, ChatMessageDefault.class);
        assertEquals(originalMessage.role(), deserializedMessage.role());
        assertEquals(originalMessage.content(), deserializedMessage.content());
    }

    @Test
    @SneakyThrows
    void deserializeInvalidRoleInChatMessageDefaultThrowsException() {
        String json = "{\"role\":\"invalid\",\"content\":\"Hello, world!\"}";
        assertThrows(ValueInstantiationException.class, () -> objectMapper.readValue(json, ChatMessageDefault.class));
    }

    @Test
    void contentSupplierProvidesCorrectContent() {
        ChatMessage message = new ChatMessageDefault(Role.USER, "Test content");
        assertEquals("Test content", message.content());
    }

    @Test
    void contentTransformerAppliesTransformation() {
        ChatMessage sourceMessage = new ChatMessageDefault(Role.USER, "Original content");
        ChatMessage transformedMessage = new ChatMessageDefault(sourceMessage, String::toUpperCase);
        assertEquals("ORIGINAL CONTENT", transformedMessage.content());
    }
}
