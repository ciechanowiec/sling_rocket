package eu.ciechanowiec.sling.rocket.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;

import java.util.*;
import java.util.stream.Stream;

/**
 * Wrapper around {@link HttpServletRequest} that provides additional functionality to the wrapped object.
 */
@SuppressWarnings("unused")
@Slf4j
public class Request implements WrappedRequest {

    private final HttpServletRequest wrappedRequest;
    private final StackTraceElement[] creationStackTrace;

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedRequest     {@link HttpServletRequest} to be wrapped by the constructed object
     * @param creationStackTrace {@link StackTraceElement} array representing the stack trace upon creation of this
     *                           object
     */
    @SuppressWarnings({"WeakerAccess", "PMD.UseVarargs"})
    public Request(
        HttpServletRequest wrappedRequest,
        StackTraceElement[] creationStackTrace
    ) {
        this.wrappedRequest = wrappedRequest;
        this.creationStackTrace = Arrays.copyOf(creationStackTrace, creationStackTrace.length);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedRequest {@link HttpServletRequest} to be wrapped by the constructed object
     */
    public Request(HttpServletRequest wrappedRequest) {
        this(wrappedRequest, new StackTraceElement[]{});
    }

    @Override
    @JsonProperty("remoteAddress")
    public String remoteAddress() {
        return wrappedRequest.getRemoteAddr();
    }

    @Override
    @JsonProperty("remoteHost")
    public String remoteHost() {
        return wrappedRequest.getRemoteHost();
    }

    @Override
    @JsonProperty("remotePort")
    public int remotePort() {
        return wrappedRequest.getRemotePort();
    }

    @Override
    @JsonProperty("remoteUser")
    public String remoteUser() {
        return wrappedRequest.getRemoteUser();
    }

    @Override
    @JsonProperty("method")
    public String method() {
        return wrappedRequest.getMethod();
    }

    @Override
    @JsonProperty("uri")
    public HttpURI uri() {
        String uri = wrappedRequest.getRequestURI();
        return HttpURI.build(uri);
    }

    @Override
    @JsonProperty("contentLength")
    public int contentLength() {
        return wrappedRequest.getContentLength();
    }

    @SuppressWarnings("unused")
    @JsonProperty("httpFields")
    Collection<HttpFieldJSON> httpFieldsJson() {
        return httpFields().stream().map(HttpFieldJSON::new).toList();
    }

    @Override
    public HttpFields httpFields() {
        HttpField[] httpFields = headerNames(wrappedRequest).map(
                headerName -> {
                    Collection<String> singleHeaderValues = headerValues(wrappedRequest, headerName);
                    return httpFields(headerName, singleHeaderValues);
                }
            ).flatMap(Collection::stream)
            .toArray(HttpField[]::new);
        return HttpFields.from(httpFields);
    }

    private Collection<HttpField> httpFields(String headerName, Collection<String> headerValues) {
        return headerValues.stream()
            .map(headerValue -> new HttpField(headerName, headerValue))
            .toList();
    }

    @Override
    @JsonProperty("wrappedRequestClass")
    public Class<?> wrappedRequestClass() {
        return wrappedRequest.getClass();
    }

    @Override
    public List<StackTraceElement> creationStackTrace() {
        return Stream.of(creationStackTrace)
            .toList();
    }

    private Stream<String> headerNames(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        Iterator<String> headerNamesIterator = headerNames.asIterator();
        return new UnwrappedIteration<>(headerNamesIterator).stream();
    }

    private Collection<String> headerValues(HttpServletRequest request, String headerName) {
        Enumeration<String> headerValues = request.getHeaders(headerName);
        Iterator<String> headerValuesIterator = headerValues.asIterator();
        return new UnwrappedIteration<>(headerValuesIterator).list();
    }

    @Override
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    public String toString() {
        return asJSON();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
