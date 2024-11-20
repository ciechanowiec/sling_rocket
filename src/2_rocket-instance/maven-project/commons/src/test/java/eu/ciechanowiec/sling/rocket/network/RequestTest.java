package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.eclipse.jetty.http.HttpURI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

@ExtendWith(SlingContextExtension.class)
@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidUsingHardCodedIP"})
class RequestTest extends TestEnvironment {

    private Resource currentResource;

    RequestTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @BeforeEach
    void setup() {
        context.build()
                .resource("/content", Map.of("universe", "Milky Way"))
                .commit();
        currentResource = context.currentResource("/content");
    }

    @Test
    @SuppressWarnings({"MagicNumber", "resource"})
    void basicTest() {
        SlingHttpServletRequest slingRequest = slingHttpServletRequest();
        Request request = new Request(
                slingRequest, stackTrace(), new UserResourceAccess(
                new AuthIDUser(slingRequest.getResourceResolver().getUserID()), fullResourceAccess
            )
        );
        String stringRepresentation = request.toString();
        assertAll(
                () -> assertEquals("/content", request.contentPath()),
                () -> assertEquals("delete", request.firstSelector().orElseThrow()),
                () -> assertEquals("file-id-00313", request.secondSelector().orElseThrow()),
                () -> assertEquals("delete.file-id-00313", request.selectorString().orElseThrow()),
                () -> assertEquals(2, request.numOfSelectors()),
                () -> assertEquals("mp4", request.extension().orElseThrow()),
                () -> assertEquals("127.0.0.1", request.remoteAddress()),
                () -> assertEquals("127.0.0.1", request.remoteHost()),
                () -> assertEquals(50_261, request.remotePort()),
                () -> assertEquals(MockJcr.DEFAULT_USER_ID, request.remoteUser()),
                () -> assertEquals(HttpConstants.METHOD_GET, request.method()),
                () -> assertEquals(HttpURI.build("/content"), request.uri()),
                () -> assertEquals(NumberUtils.INTEGER_ZERO, request.contentLength()),
                () -> assertEquals(3, request.httpFields().size()),
                () -> assertEquals(MockSlingHttpServletRequest.class, request.wrappedRequestClass()),
                () -> assertTrue(request.creationStackTrace().size() > NumberUtils.INTEGER_ZERO),
                () -> assertEquals(currentResource, request.resource()),
                () -> assertNotNull(request.userResourceAccess().acquireAccess().getResource("/content")),
                () -> assertTrue(stringRepresentation.contains("gorgus")),
                () -> assertTrue(stringRepresentation.contains("valus"))
        );
    }

    @Test
    void noRequestPathInfo() {
        MockSlingHttpServletRequest slingRequest = context.request();
        Request request = new Request(
                slingRequest, new UserResourceAccess(
                new AuthIDUser(slingHttpServletRequest().getResourceResolver().getUserID()), fullResourceAccess
            )
        );
        assertAll(
                () -> assertTrue(request.firstSelector().isEmpty()),
                () -> assertTrue(request.secondSelector().isEmpty()),
                () -> assertTrue(request.selectorString().isEmpty()),
                () -> assertEquals(NumberUtils.INTEGER_ZERO, request.numOfSelectors()),
                () -> assertTrue(request.extension().isEmpty())
        );
    }

    @SuppressWarnings({
            "IllegalCatch", "ThrowCaughtLocally", "PMD.AvoidThrowingRawExceptionTypes", "PMD.ExceptionAsFlowControl",
            "PMD.AvoidCatchingGenericException"
    })
    private StackTraceElement[] stackTrace() {
        try {
            throw new RuntimeException("For stack trace");
        } catch (RuntimeException exception) {
            return exception.getStackTrace();
        }
    }

    @SuppressWarnings("MagicNumber")
    private SlingHttpServletRequest slingHttpServletRequest() {
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath("/content");
        mockRequestPathInfo.setSelectorString("delete.file-id-00313");
        mockRequestPathInfo.setExtension("mp4");
        MockSlingHttpServletRequest request = spy(context.request());
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteAddr("127.0.0.1");
        request.setRemoteHost("127.0.0.1");
        request.setRemotePort(50_261);
        request.setRemoteUser("admin");
        request.setMethod(HttpConstants.METHOD_GET);
        request.setContent(new byte[]{});
        request.addHeader("namus", "valus");
        request.addHeader("namus", "gorgus");
        request.addHeader("duos", "polus");
        return request;
    }
}
