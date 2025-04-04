package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.asset.Asset;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP response with an {@link Asset}.
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
@ToString
public class ResponseWithAsset {

    private final HttpServletResponse wrappedResponse;
    private final Asset assetToSend;
    private final AtomicBoolean wasSent;

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedResponse {@link HttpServletResponse} which will be used to send this {@link ResponseWithAsset}
     * @param assetToSend     {@link Asset} that will be sent with this {@link Response}
     */
    public ResponseWithAsset(HttpServletResponse wrappedResponse, Asset assetToSend) {
        this.wrappedResponse = wrappedResponse;
        this.assetToSend = assetToSend;
        this.wasSent = new AtomicBoolean(false);
    }

    /**
     * Respond the client to an HTTP request via sending this HTTP {@link ResponseWithAsset}.
     * <p>
     * This method can be called only once for a given object. If called more than once or the response has been already
     * committed as specified by {@link ServletResponse#isCommitted()}, an {@link AlreadySentException} is thrown.
     *
     * @param contentDispositionHeader {@link ContentDispositionHeader} to be sent with this {@link ResponseWithAsset}
     * @throws AlreadySentException if this {@link Response} has already been sent or the response has been already
     *                              committed as specified by {@link ServletResponse#isCommitted()}
     */
    @SneakyThrows
    public void send(ContentDispositionHeader contentDispositionHeader) {
        boolean isAllowed = !wasSent.get() && !wrappedResponse.isCommitted();
        Conditional.isTrueOrThrow(isAllowed, new AlreadySentException(this));
        assetToSend.assetFile().retrieve().ifPresentOrElse(
            file -> send(file, contentDispositionHeader),
            () -> new Response(
                wrappedResponse, new Status(HttpServletResponse.SC_NOT_FOUND, "No asset found")
            ).send()
        );
    }

    @SneakyThrows
    private void send(File fileToSend, ContentDispositionHeader contentDispositionHeader) {
        log.trace("Sending {}", fileToSend);
        int length = (int) fileToSend.length();
        wrappedResponse.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        wrappedResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDispositionHeader.value(fileToSend));
        wrappedResponse.setContentLength(length);
        try (InputStream inputStream = Files.newInputStream(fileToSend.toPath())) {
            IOUtils.copy(inputStream, wrappedResponse.getOutputStream());
        }
        wrappedResponse.flushBuffer();
        wasSent.set(true);
        log.trace("Sent {}", this);
    }
}
