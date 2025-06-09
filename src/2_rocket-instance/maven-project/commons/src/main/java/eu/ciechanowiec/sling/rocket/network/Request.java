package eu.ciechanowiec.sling.rocket.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.FileWithOriginalName;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;

/**
 * Wrapper around {@link SlingHttpServletRequest} that provides additional functionality to the wrapped object.
 */
@SuppressWarnings(
    {
        "WeakerAccess", "MethodCount", "ClassWithTooManyMethods", "PMD.ExcessivePublicCount", "PMD.TooManyMethods",
        "PMD.CouplingBetweenObjects", "PMD.ExcessiveImports"
    }
)
@Slf4j
public class Request implements RequestWithDecomposition, RequestWithFiles, JSON {

    private final SlingHttpServletRequest wrappedRequest;
    private final StackTraceElement[] creationStackTrace;
    private final UserResourceAccess userResourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedRequest     {@link SlingHttpServletRequest} to be wrapped by the constructed object
     * @param creationStackTrace {@link StackTraceElement} array representing the stack trace upon creation of this
     *                           object
     * @param userResourceAccess {@link UserResourceAccess} for the {@link User} who issued the wrapped
     *                           {@link SlingHttpServletRequest}
     */
    public Request(
        SlingHttpServletRequest wrappedRequest,
        StackTraceElement[] creationStackTrace,
        UserResourceAccess userResourceAccess
    ) {
        this.wrappedRequest = wrappedRequest;
        this.creationStackTrace = Arrays.copyOf(creationStackTrace, creationStackTrace.length);
        this.userResourceAccess = userResourceAccess;
    }

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedRequest     {@link SlingHttpServletRequest} to be wrapped by the constructed object
     * @param userResourceAccess {@link UserResourceAccess} for the {@link User} who issued the wrapped
     *                           {@link SlingHttpServletRequest}
     */
    public Request(SlingHttpServletRequest wrappedRequest, UserResourceAccess userResourceAccess) {
        this(wrappedRequest, new StackTraceElement[]{}, userResourceAccess);
    }

    @Override
    public String contentPath() {
        return wrappedRequest.getRequestPathInfo().getResourcePath();
    }

    @Override
    public Optional<String> firstSelector() {
        List<String> selectors = List.of(wrappedRequest.getRequestPathInfo().getSelectors());
        return selectors.stream().findFirst();
    }

    @Override
    @SuppressWarnings({"unchecked", "squid:S1612", "PMD.LambdaCanBeMethodReference"})
    public Optional<String> secondSelector() {
        List<String> selectors = List.of(wrappedRequest.getRequestPathInfo().getSelectors());
        return Conditional.conditional(selectors.size() >= NumberUtils.INTEGER_TWO)
            .onTrue(() -> Optional.of(selectors.get(NumberUtils.INTEGER_ONE)))
            .onFalse(() -> Optional.empty())
            .get(Optional.class);
    }

    @Override
    @SuppressWarnings({"unchecked", "squid:S1612", "PMD.LambdaCanBeMethodReference"})
    public Optional<String> thirdSelector() {
        List<String> selectors = List.of(wrappedRequest.getRequestPathInfo().getSelectors());
        return Conditional.conditional(selectors.size() >= 3)
            .onTrue(() -> Optional.of(selectors.get(NumberUtils.INTEGER_TWO)))
            .onFalse(() -> Optional.empty())
            .get(Optional.class);
    }

    @Override
    public Optional<String> selectorString() {
        return Optional.ofNullable(wrappedRequest.getRequestPathInfo().getSelectorString());
    }

    @Override
    public int numOfSelectors() {
        return wrappedRequest.getRequestPathInfo().getSelectors().length;
    }

    @Override
    public Optional<String> extension() {
        return Optional.ofNullable(wrappedRequest.getRequestPathInfo().getExtension());
    }

    /**
     * Returns the value returned by {@link ServletRequest#getRemoteAddr()} for the wrapped
     * {@link SlingHttpServletRequest}.
     *
     * @return value returned by {@link ServletRequest#getRemoteAddr()} for the wrapped {@link SlingHttpServletRequest}
     */
    @JsonProperty("remoteAddress")
    public String remoteAddress() {
        return wrappedRequest.getRemoteAddr();
    }

    /**
     * Returns the value returned by {@link ServletRequest#getRemoteHost()} for the wrapped
     * {@link SlingHttpServletRequest}.
     *
     * @return value returned by {@link ServletRequest#getRemoteHost()} for the wrapped {@link SlingHttpServletRequest}
     */
    @JsonProperty("remoteHost")
    public String remoteHost() {
        return wrappedRequest.getRemoteHost();
    }

    /**
     * Returns the value returned by {@link ServletRequest#getRemotePort()} for the wrapped
     * {@link SlingHttpServletRequest}.
     *
     * @return value returned by {@link ServletRequest#getRemotePort()} for the wrapped {@link SlingHttpServletRequest}
     */
    @JsonProperty("remotePort")
    public int remotePort() {
        return wrappedRequest.getRemotePort();
    }

    /**
     * Returns the value returned by {@link SlingHttpServletRequest#getRemoteUser()} for the wrapped
     * {@link SlingHttpServletRequest}.
     *
     * @return value returned by {@link SlingHttpServletRequest#getRemoteUser()} for the wrapped
     * {@link SlingHttpServletRequest}
     */
    @JsonProperty("remoteUser")
    public String remoteUser() {
        return wrappedRequest.getRemoteUser();
    }

    /**
     * Returns the value returned by {@link HttpServletRequest#getMethod()} for the wrapped
     * {@link SlingHttpServletRequest}.
     *
     * @return value returned by {@link HttpServletRequest#getMethod()} for the wrapped {@link SlingHttpServletRequest}
     */
    @JsonProperty("method")
    public String method() {
        return wrappedRequest.getMethod();
    }

    /**
     * Returns the value returned by {@link HttpServletRequest#getRequestURI()} for the wrapped
     * {@link SlingHttpServletRequest}.
     *
     * @return value returned by {@link HttpServletRequest#getRequestURI()} for the wrapped
     * {@link SlingHttpServletRequest}
     */
    @JsonProperty("uri")
    public HttpURI uri() {
        String uri = wrappedRequest.getRequestURI();
        return HttpURI.build(uri);
    }

    /**
     * Returns the value returned by {@link ServletRequest#getContentLength()} for the wrapped
     * {@link SlingHttpServletRequest}.
     *
     * @return value returned by {@link ServletRequest#getContentLength()} for the wrapped
     * {@link SlingHttpServletRequest}
     */
    @JsonProperty("contentLength")
    public int contentLength() {
        return wrappedRequest.getContentLength();
    }

    @JsonProperty("httpFields")
    Collection<HttpFieldJSON> httpFieldsJson() {
        return httpFields().stream().map(HttpFieldJSON::new).toList();
    }

    /**
     * Returns all {@link HttpFields} of the wrapped {@link SlingHttpServletRequest}.
     *
     * @return all {@link HttpFields} of the wrapped {@link SlingHttpServletRequest}
     */
    public HttpFields httpFields() {
        HttpField[] httpFields = headerNames(wrappedRequest).map(
                headerName -> {
                    Collection<String> singleHeaderValues = headerValues(wrappedRequest, headerName);
                    return httpFields(headerName, singleHeaderValues);
                }
            ).flatMap(Collection::stream)
            .toArray(HttpField[]::new);
        return new HttpFields.Immutable(httpFields);
    }

    private Collection<HttpField> httpFields(String headerName, Collection<String> headerValues) {
        return headerValues.stream()
            .map(headerValue -> new HttpField(headerName, headerValue))
            .toList();
    }

    /**
     * Returns the {@link Class} of the wrapped {@link SlingHttpServletRequest}.
     *
     * @return {@link Class} of the wrapped {@link SlingHttpServletRequest}
     */
    @JsonProperty("wrappedRequestClass")
    public Class<?> wrappedRequestClass() {
        return wrappedRequest.getClass();
    }

    /**
     * Returns the {@link List} of {@link StackTraceElement}s representing the stack trace upon creation of this
     * object.
     * <p>
     * The {@link List} is empty if the stack trace was not provided upon object creation.
     *
     * @return {@link List} of {@link StackTraceElement}s representing the stack trace upon creation of this object; the
     * {@link List} is empty if the stack trace was not provided upon object creation
     */
    public List<StackTraceElement> creationStackTrace() {
        return Stream.of(creationStackTrace)
            .toList();
    }

    /**
     * Returns the {@link Resource} returned by the wrapped {@link SlingHttpServletRequest#getResource()}.
     *
     * @return {@link Resource} returned by the wrapped {@link SlingHttpServletRequest#getResource()}
     */
    @JsonProperty("resource")
    public Resource resource() {
        return new ResourceJSON(wrappedRequest.getResource());
    }

    /**
     * Returns {@link UserResourceAccess} for the {@link User} who issued the wrapped {@link SlingHttpServletRequest}.
     *
     * @return {@link UserResourceAccess} for the {@link User} who issued the wrapped {@link SlingHttpServletRequest}
     */
    public UserResourceAccess userResourceAccess() {
        return userResourceAccess;
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

    @Override
    @SneakyThrows
    public List<FileWithOriginalName> uploadedFiles() {
        log.trace("Extracting files from: {}", this);
        Map<String, RequestParameter[]> requestParams = wrappedRequest.getRequestParameterMap();
        log.trace("Request params from {} are {}. Number of params: {}", this, requestParams, requestParams.size());
        return requestParams.values()
            .stream()
            .flatMap(Stream::of)
            .map(this::asTempFile)
            .flatMap(Optional::stream)
            .toList();
    }

    @SneakyThrows
    @SuppressWarnings("squid:S1905")
    private Optional<FileWithOriginalName> asTempFile(RequestParameter requestParameter) {
        log.trace("Attempting to convert a request parameter '{}' to a file", requestParameter.getName());
        if (requestParameter.isFormField()) {
            log.trace(
                "Request parameter '{}' is a simple form field and will not be converted to a file",
                requestParameter.getName()
            );
            return Optional.empty();
        }
        try (InputStream inputStreamNullable = requestParameter.getInputStream()) {
            return Optional.ofNullable(inputStreamNullable)
                .filter(inputStream -> Objects.nonNull(requestParameter.getFileName()))
                .map(inputStream -> {
                    String fileName = Objects.requireNonNull(requestParameter.getFileName());
                    File tempFile = asTempFile(inputStream, fileName);
                    return new FileWithOriginalName(tempFile, fileName);
                });
        }
    }

    @SneakyThrows
    private File asTempFile(InputStream inputStream, String fileNamePrefix) {
        File tempFile = File.createTempFile(fileNamePrefix, ".tmp");
        Path tempFilePath = tempFile.toPath();
        Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        log.trace("Created {}", tempFile);
        return tempFile;
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
