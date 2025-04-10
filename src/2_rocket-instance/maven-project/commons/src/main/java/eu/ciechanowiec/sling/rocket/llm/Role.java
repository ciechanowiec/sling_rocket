package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * Represents the type of the author of a {@link ChatMessage}.
 */
public enum Role {

    /**
     * {@code developer} type of the author of a {@link ChatMessage}.
     */
    DEVELOPER,

    /**
     * {@code tool} type of the author of a {@link ChatMessage}.
     */
    TOOL,

    /**
     * {@code system} type of the author of a {@link ChatMessage}.
     */
    SYSTEM,

    /**
     * {@code user} type of the author of a {@link ChatMessage}.
     */
    USER,

    /**
     * {@code assistant} type of the author of a {@link ChatMessage}.
     */
    ASSISTANT,

    /**
     * {@code unknown} type of the author of a {@link ChatMessage}.
     */
    UNKNOWN;

    /**
     * Returns the name of this enum constant in lowercase.
     *
     * @return name of this enum constant in lowercase
     */
    @Override
    public String toString() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Returns the name of this enum constant in lowercase.
     *
     * @return name of this enum constant in lowercase
     */
    @JsonValue
    public String toLower() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Transforms the specified {@code value} to uppercase, passes it to {@link Role#valueOf(String)} and returns the
     * result produced by that method. Mainly should be used for deserialization.
     *
     * @param value the name of the enum constant to be returned
     * @return the enum constant related with the specified name
     */
    @JsonCreator
    public static Role ofValue(String value) {
        String upperCaseValue = value.toUpperCase(Locale.ENGLISH);
        return Role.valueOf(upperCaseValue);
    }
}
