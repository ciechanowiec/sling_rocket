package eu.ciechanowiec.sling.rocket.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP response.
 */
@SuppressWarnings("WeakerAccess")
@ToString
@Slf4j
public class Response implements JSON {

    @JsonProperty("status")
    private final Status status;

    @JsonProperty("affected")
    private final List<Affected> affected;

    private final HttpServletResponse wrappedResponse;
    private final AtomicBoolean wasSent;

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedResponse {@link HttpServletResponse} which will be used to send this {@link Response}
     * @param status          {@link Status} that will be sent with this {@link Response}
     */
    @SuppressWarnings("unused")
    public Response(HttpServletResponse wrappedResponse, Status status) {
        this(wrappedResponse, status, Collections.emptyList());
    }

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedResponse {@link HttpServletResponse} which will be used to send this {@link Response}
     * @param status          {@link Status} that will be sent with this {@link Response}
     * @param affected        {@link List} of {@link Affected} instances related to this {@link Response}
     */
    public Response(HttpServletResponse wrappedResponse, Status status, List<Affected> affected) {
        this.status = status;
        this.affected = Collections.unmodifiableList(affected);
        this.wrappedResponse = wrappedResponse;
        this.wasSent = new AtomicBoolean(false);
    }

    /**
     * Respond the client to an HTTP request via sending this HTTP {@link Response}.
     * <p>
     * This method can be called only once for a given object. If called more than once or the response has been already
     * committed as specified by {@link ServletResponse#isCommitted()}, an {@link AlreadySentException} is thrown.
     *
     * @throws AlreadySentException if this {@link Response} has already been sent or the response has been already
     *                              committed as specified by {@link ServletResponse#isCommitted()}
     */
    @SneakyThrows
    public void send() {
        boolean isAllowed = !wasSent.get() && !wrappedResponse.isCommitted();
        Conditional.isTrueOrThrow(isAllowed, new AlreadySentException(this));
        try (PrintWriter responseWriter = wrappedResponse.getWriter()) {
            wrappedResponse.setStatus(status.code());
            wrappedResponse.setContentType(MediaType.APPLICATION_JSON);
            responseWriter.write(asJSON());
            responseWriter.flush();
        }
        wrappedResponse.flushBuffer();
        wasSent.set(true);
        log.trace("Sent {}", this);
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
