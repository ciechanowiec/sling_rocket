package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.conditional.Conditional;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP response with an HTML page.
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
@ToString
public class ResponseWithHTML {

    private final HttpServletResponse wrappedResponse;
    private final String htmlToSend;
    private final AtomicBoolean wasSent;
    private final int httpStatusCode;

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedResponse {@link HttpServletResponse} which will be used to send this {@link ResponseWithHTML}
     * @param htmlToSend      HTML {@link String} that will be sent with this {@link Response}
     * @param httpStatusCode  HTTP status code to be sent with this {@link ResponseWithHTML}
     */
    public ResponseWithHTML(HttpServletResponse wrappedResponse, String htmlToSend, int httpStatusCode) {
        this.wrappedResponse = wrappedResponse;
        this.htmlToSend = htmlToSend;
        this.wasSent = new AtomicBoolean(false);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Respond the client to an HTTP request via sending this HTTP {@link ResponseWithHTML}.
     * <p>
     * This method can be called only once for a given object. If called more than once or the response has been already
     * committed as specified by {@link ServletResponse#isCommitted()}, an {@link AlreadySentException} is thrown.
     *
     * @throws AlreadySentException if this {@link Response} has already been sent or the response has been already
     *                              committed as specified by {@link ServletResponse#isCommitted()}
     */
    @SneakyThrows
    public void send() {
        int htmlLength = htmlToSend.length();
        log.trace("Sending HTML of length {}", htmlLength);
        boolean isAllowed = !wasSent.get() && !wrappedResponse.isCommitted();
        Conditional.isTrueOrThrow(isAllowed, new AlreadySentException(this));
        try (PrintWriter printWriter = wrappedResponse.getWriter()) {
            wrappedResponse.setStatus(httpStatusCode);
            wrappedResponse.setContentType(MediaType.TEXT_HTML);
            printWriter.write(htmlToSend);
            printWriter.flush();
        }
        wrappedResponse.flushBuffer();
    }
}
