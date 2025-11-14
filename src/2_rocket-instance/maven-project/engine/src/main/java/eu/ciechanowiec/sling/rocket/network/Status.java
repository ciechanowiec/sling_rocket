package eu.ciechanowiec.sling.rocket.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

/**
 * Status of processing of an HTTP request.
 */
@ToString
public class Status {

    @JsonProperty("code")
    @Getter
    private final int code;

    @SuppressWarnings("unused")
    @JsonProperty("message")
    private final String message;

    /**
     * Constructs an instance of this class.
     *
     * @param code    HTTP error code
     * @param message explanation why this {@link Status} occurred
     */
    @SuppressWarnings("WeakerAccess")
    public Status(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
