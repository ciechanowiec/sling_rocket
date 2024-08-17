package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("MultipleStringLiterals")
class RoleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void roleToStringReturnsLowerCase() {
        assertEquals("system", Role.SYSTEM.toString());
        assertEquals("user", Role.USER.toString());
        assertEquals("assistant", Role.ASSISTANT.toString());
        assertEquals("unknown", Role.UNKNOWN.toString());
    }

    @Test
    void roleToLowerReturnsLowerCase() {
        assertEquals("system", Role.SYSTEM.toLower());
        assertEquals("user", Role.USER.toLower());
        assertEquals("assistant", Role.ASSISTANT.toLower());
        assertEquals("unknown", Role.UNKNOWN.toLower());
    }

    @Test
    void ofValueReturnsCorrectRole() {
        assertEquals(Role.SYSTEM, Role.ofValue("system"));
        assertEquals(Role.USER, Role.ofValue("user"));
        assertEquals(Role.ASSISTANT, Role.ofValue("assistant"));
        assertEquals(Role.UNKNOWN, Role.ofValue("unknown"));
    }

    @Test
    void ofValueIsCaseInsensitive() {
        assertEquals(Role.SYSTEM, Role.ofValue("SYSTEM"));
        assertEquals(Role.USER, Role.ofValue("USER"));
        assertEquals(Role.ASSISTANT, Role.ofValue("ASSISTANT"));
        assertEquals(Role.UNKNOWN, Role.ofValue("UNKNOWN"));
    }

    @Test
    void ofValueThrowsExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> Role.ofValue("invalid"));
    }

    @Test
    @SneakyThrows
    void serializeRoleToJson() {
        String json = objectMapper.writeValueAsString(Role.SYSTEM);
        assertEquals("\"system\"", json);
    }

    @Test
    @SneakyThrows
    void deserializeJsonToRole() {
        Role roleLowerCase = objectMapper.readValue("\"system\"", Role.class);
        Role roleUpperCase = objectMapper.readValue("\"SYSTEM\"", Role.class);
        assertEquals(Role.SYSTEM, roleLowerCase);
        assertEquals(Role.SYSTEM, roleUpperCase);
    }

    @Test
    @SneakyThrows
    void serializeAndDeserializeRole() {
        String json = objectMapper.writeValueAsString(Role.USER);
        Role role = objectMapper.readValue(json, Role.class);
        assertEquals(Role.USER, role);
    }

    @Test
    void deserializeInvalidRoleThrowsException() {
        assertThrows(ValueInstantiationException.class, () -> objectMapper.readValue("\"invalid\"", Role.class));
    }
}
