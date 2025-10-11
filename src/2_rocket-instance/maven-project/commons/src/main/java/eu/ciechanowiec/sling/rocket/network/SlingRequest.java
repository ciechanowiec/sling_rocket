package eu.ciechanowiec.sling.rocket.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.FileWithOriginalName;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

/**
 * Wrapper around {@link SlingJakartaHttpServletRequest} that provides additional functionality to the wrapped object.
 */
@SuppressWarnings(
    {"ClassWithTooManyMethods", "WeakerAccess", "MethodCount", "PMD.TooManyMethods", "PMD.ExcessivePublicCount"}
)
@Slf4j
public class SlingRequest implements WrappedSlingRequest {

    private final Request wrappedRequest;
    private final SlingJakartaHttpServletRequest wrappedSlingRequest;
    private final UserResourceAccess userResourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedSlingRequest {@link SlingJakartaHttpServletRequest} to be wrapped by the constructed object
     * @param creationStackTrace  {@link StackTraceElement} array representing the stack trace upon creation of this
     *                            object
     * @param userResourceAccess  {@link UserResourceAccess} for the {@link User} who issued the wrapped
     *                            {@link SlingJakartaHttpServletRequest}
     */
    public SlingRequest(
        SlingJakartaHttpServletRequest wrappedSlingRequest,
        StackTraceElement[] creationStackTrace,
        UserResourceAccess userResourceAccess
    ) {
        this.wrappedRequest = new Request(wrappedSlingRequest, creationStackTrace);
        this.wrappedSlingRequest = wrappedSlingRequest;
        this.userResourceAccess = userResourceAccess;
    }

    /**
     * Constructs an instance of this class.
     *
     * @param wrappedSlingRequest {@link SlingJakartaHttpServletRequest} to be wrapped by the constructed object
     * @param userResourceAccess  {@link UserResourceAccess} for the {@link User} who issued the wrapped
     *                            {@link SlingJakartaHttpServletRequest}
     */
    public SlingRequest(SlingJakartaHttpServletRequest wrappedSlingRequest, UserResourceAccess userResourceAccess) {
        this(wrappedSlingRequest, new StackTraceElement[]{}, userResourceAccess);
    }

    @Override
    public String contentPath() {
        return wrappedSlingRequest.getRequestPathInfo().getResourcePath();
    }

    @Override
    public Optional<String> firstSelector() {
        List<String> selectors = List.of(wrappedSlingRequest.getRequestPathInfo().getSelectors());
        return selectors.stream().findFirst();
    }

    @Override
    @SuppressWarnings({"unchecked", "squid:S1612", "PMD.LambdaCanBeMethodReference"})
    public Optional<String> secondSelector() {
        List<String> selectors = List.of(wrappedSlingRequest.getRequestPathInfo().getSelectors());
        return Conditional.conditional(selectors.size() >= NumberUtils.INTEGER_TWO)
            .onTrue(() -> Optional.of(selectors.get(NumberUtils.INTEGER_ONE)))
            .onFalse(() -> Optional.empty())
            .get(Optional.class);
    }

    @Override
    @SuppressWarnings({"unchecked", "squid:S1612", "PMD.LambdaCanBeMethodReference"})
    public Optional<String> thirdSelector() {
        List<String> selectors = List.of(wrappedSlingRequest.getRequestPathInfo().getSelectors());
        return Conditional.conditional(selectors.size() >= 3)
            .onTrue(() -> Optional.of(selectors.get(NumberUtils.INTEGER_TWO)))
            .onFalse(() -> Optional.empty())
            .get(Optional.class);
    }

    @Override
    public Optional<String> selectorString() {
        return Optional.ofNullable(wrappedSlingRequest.getRequestPathInfo().getSelectorString());
    }

    @Override
    public int numOfSelectors() {
        return wrappedSlingRequest.getRequestPathInfo().getSelectors().length;
    }

    @Override
    public Optional<String> extension() {
        return Optional.ofNullable(wrappedSlingRequest.getRequestPathInfo().getExtension());
    }

    @Override
    @JsonProperty("remoteAddress")
    public String remoteAddress() {
        return wrappedRequest.remoteAddress();
    }

    @Override
    @JsonProperty("remoteHost")
    public String remoteHost() {
        return wrappedRequest.remoteHost();
    }

    @Override
    @JsonProperty("remotePort")
    public int remotePort() {
        return wrappedRequest.remotePort();
    }

    @Override
    @JsonProperty("remoteUser")
    public String remoteUser() {
        return wrappedSlingRequest.getRemoteUser();
    }

    @Override
    @JsonProperty("method")
    public String method() {
        return wrappedRequest.method();
    }

    @Override
    @JsonProperty("uri")
    public HttpURI uri() {
        return wrappedRequest.uri();
    }

    @Override
    @JsonProperty("contentLength")
    public int contentLength() {
        return wrappedRequest.contentLength();
    }

    @SuppressWarnings("unused")
    @JsonProperty("httpFields")
    Collection<HttpFieldJSON> httpFieldsJson() {
        return wrappedRequest.httpFieldsJson();
    }

    @Override
    public HttpFields httpFields() {
        return wrappedRequest.httpFields();
    }

    @Override
    @JsonProperty("wrappedRequestClass")
    public Class<?> wrappedRequestClass() {
        return wrappedSlingRequest.getClass();
    }

    @Override
    public List<StackTraceElement> creationStackTrace() {
        return wrappedRequest.creationStackTrace();
    }

    @Override
    @JsonProperty("resource")
    public Resource resource() {
        return new ResourceJSON(wrappedSlingRequest.getResource());
    }

    @Override
    public UserResourceAccess userResourceAccess() {
        return userResourceAccess;
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
        Map<String, RequestParameter[]> requestParams = wrappedSlingRequest.getRequestParameterMap();
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

    @Override
    public Optional<String> suffix() {
        return Optional.ofNullable(wrappedSlingRequest.getRequestPathInfo().getSuffix());
    }
}
