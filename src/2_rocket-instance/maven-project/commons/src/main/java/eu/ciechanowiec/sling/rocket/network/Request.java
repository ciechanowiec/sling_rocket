package eu.ciechanowiec.sling.rocket.network;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper around {@link SlingHttpServletRequest} that provides additional functionality to the wrapped object.
 */
@SuppressWarnings({
        "WeakerAccess", "MethodCount", "ClassWithTooManyMethods", "PMD.ExcessivePublicCount", "PMD.TooManyMethods"
})
public class Request implements RequestWithDecomposition {

    private final SlingHttpServletRequest wrappedRequest;
    private final StackTraceElement[] creationStackTrace;
    private final UserResourceAccess userResourceAccess;

    /**
     * Constructs an instance of this class.
     * @param wrappedRequest {@link SlingHttpServletRequest} to be wrapped by the constructed object
     * @param creationStackTrace {@link StackTraceElement} array representing the stack trace
     *                           upon creation of this object
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
     * @param wrappedRequest {@link SlingHttpServletRequest} to be wrapped by the constructed object
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
     * Returns the value returned by {@link ServletRequest#getRemoteAddr()}
     * for the wrapped {@link SlingHttpServletRequest}.
     * @return value returned by {@link ServletRequest#getRemoteAddr()} for the wrapped {@link SlingHttpServletRequest}
     */
    public String remoteAddress() {
        return wrappedRequest.getRemoteAddr();
    }

    /**
     * Returns the value returned by {@link ServletRequest#getRemoteHost()}
     * for the wrapped {@link SlingHttpServletRequest}.
     * @return value returned by {@link ServletRequest#getRemoteHost()} for the wrapped {@link SlingHttpServletRequest}
     */
    public String remoteHost() {
        return wrappedRequest.getRemoteHost();
    }

    /**
     * Returns the value returned by {@link ServletRequest#getRemotePort()}
     * for the wrapped {@link SlingHttpServletRequest}.
     * @return value returned by {@link ServletRequest#getRemoteHost()} for the wrapped {@link SlingHttpServletRequest}
     */
    public int remotePort() {
        return wrappedRequest.getRemotePort();
    }

    /**
     * Returns the value returned by {@link SlingHttpServletRequest#getRemoteUser()}
     * for the wrapped {@link SlingHttpServletRequest}.
     * @return value returned by {@link SlingHttpServletRequest#getRemoteUser()}
     *         for the wrapped {@link SlingHttpServletRequest}
     */
    public String remoteUser() {
        return wrappedRequest.getRemoteUser();
    }

    /**
     * Returns the value returned by {@link HttpServletRequest#getMethod()}
     * for the wrapped {@link SlingHttpServletRequest}.
     * @return value returned by {@link HttpServletRequest#getMethod()} for the wrapped {@link SlingHttpServletRequest}
     */
    public String method() {
        return wrappedRequest.getMethod();
    }

    /**
     * Returns the value returned by {@link HttpServletRequest#getRequestURI()}
     * for the wrapped {@link SlingHttpServletRequest}.
     * @return value returned by {@link HttpServletRequest#getRequestURI()}
     *         for the wrapped {@link SlingHttpServletRequest}
     */
    public HttpURI uri() {
        String uri = wrappedRequest.getRequestURI();
        return HttpURI.build(uri);
    }

    /**
     * Returns the value returned by {@link ServletRequest#getContentLength()}
     * for the wrapped {@link SlingHttpServletRequest}.
     * @return value returned by {@link ServletRequest#getContentLength()}
     *         for the wrapped {@link SlingHttpServletRequest}
     */
    public int contentLength() {
        return wrappedRequest.getContentLength();
    }

    /**
     * Returns all {@link HttpFields} of the wrapped {@link SlingHttpServletRequest}.
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
     * @return {@link Class} of the wrapped {@link SlingHttpServletRequest}
     */
    public Class<?> wrappedRequestClass() {
        return wrappedRequest.getClass();
    }

    /**
     * Returns the {@link List} of {@link StackTraceElement}s representing the stack trace upon creation of this object.
     * <p>
     * The {@link List} is empty if the stack trace was not provided upon object creation.
     * @return {@link List} of {@link StackTraceElement}s representing the stack trace upon creation of this object;
     *         the {@link List} is empty if the stack trace was not provided upon object creation
     */
    public List<StackTraceElement> creationStackTrace() {
        return Stream.of(creationStackTrace)
                     .toList();
    }

    /**
     * Returns the {@link Resource} returned by the wrapped {@link SlingHttpServletRequest#getResource()}.
     * @return {@link Resource} returned by the wrapped {@link SlingHttpServletRequest#getResource()}
     */
    public Resource resource() {
        return wrappedRequest.getResource();
    }

    /**
     * Returns {@link UserResourceAccess} for the {@link User}
     * who issued the wrapped {@link SlingHttpServletRequest}.
     * @return {@link UserResourceAccess} for the {@link User}
     *         who issued the wrapped {@link SlingHttpServletRequest}
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
        return """
               vvvvvvv HTTP REQUEST START vvvvvvv
               > REQUEST CLASS: %s
               > REMOTE ADDRESS: %s
               > REMOTE HOST: %s
               > REMOTE PORT: %d
               > REMOTE USER: %s
               > METHOD: %s
               > URI: %s
               > CONTENT LENGTH: %s
               > SLING RESOURCE: %s
               > HTTP FIELDS: %n%s
               > CREATION STACK TRACE: %n%s
               ^^^^^^^ HTTP REQUEST END ^^^^^^^""".formatted(
                wrappedRequestClass(),
                remoteAddress(),
                remoteHost(),
                remotePort(),
                remoteUser(),
                method(),
                uri(),
                contentLength(),
                resource(),
                toString(httpFields()),
                toString(creationStackTrace().stream())
        );
    }

    private String toString(HttpFields httpFields) {
        return toString(httpFields.stream());
    }

    private String toString(Stream<?> stream) {
        return stream.map(streamElement -> String.format("---> %s", streamElement))
                .collect(Collectors.joining(StringUtils.LF));
    }
}
