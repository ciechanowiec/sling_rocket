package eu.ciechanowiec.sling.rocket.network;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.FileWithOriginalName;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.eclipse.jetty.http.HttpURI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

@ExtendWith(SlingContextExtension.class)
@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidUsingHardCodedIP"})
class SlingRequestTest extends TestEnvironment {

    private Resource currentResource;

    SlingRequestTest() {
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
    @SuppressWarnings("MagicNumber")
    void basicTest() {
        SlingJakartaHttpServletRequest mockedSlingRequest = slingHttpServletRequest();
        SlingRequest slingRequest = new SlingRequest(
            mockedSlingRequest, stackTrace(), new UserResourceAccess(
            new AuthIDUser(mockedSlingRequest.getResourceResolver().getUserID()), fullResourceAccess
        )
        );
        String stringRepresentation = slingRequest.toString();
        assertAll(
            () -> assertEquals("/content", slingRequest.contentPath()),
            () -> assertEquals("delete", slingRequest.firstSelector().orElseThrow()),
            () -> assertEquals("file-id-00313", slingRequest.secondSelector().orElseThrow()),
            () -> assertTrue(slingRequest.thirdSelector().isEmpty()),
            () -> assertEquals("delete.file-id-00313", slingRequest.selectorString().orElseThrow()),
            () -> assertEquals(2, slingRequest.numOfSelectors()),
            () -> assertEquals("mp4", slingRequest.extension().orElseThrow()),
            () -> assertEquals("127.0.0.1", slingRequest.remoteAddress()),
            () -> assertEquals("127.0.0.1", slingRequest.remoteHost()),
            () -> assertEquals(50_261, slingRequest.remotePort()),
            () -> assertEquals(MockJcr.DEFAULT_USER_ID, slingRequest.remoteUser()),
            () -> assertEquals(HttpConstants.METHOD_GET, slingRequest.method()),
            () -> assertEquals(HttpURI.build("/content"), slingRequest.uri()),
            () -> assertEquals(NumberUtils.INTEGER_ZERO, slingRequest.contentLength()),
            () -> assertEquals(3, slingRequest.httpFields().size()),
            () -> assertEquals(MockSlingJakartaHttpServletRequest.class, slingRequest.wrappedRequestClass()),
            () -> assertTrue(slingRequest.creationStackTrace().size() > NumberUtils.INTEGER_ZERO),
            () -> assertEquals(currentResource.getPath(), slingRequest.resource().getPath()),
            () -> assertNotNull(slingRequest.userResourceAccess().acquireAccess().getResource("/content")),
            () -> assertTrue(stringRepresentation.contains("gorgus")),
            () -> assertTrue(stringRepresentation.contains("valus"))
        );
    }

    @Test
    void testRequestWithNoFiles() {
        MockSlingJakartaHttpServletRequest slingRequest = context.jakartaRequest();
        WrappedSlingRequest requestWithFiles = new SlingRequest(
            slingRequest, new UserResourceAccess(
            new AuthIDUser(slingRequest.getResourceResolver().getUserID()), fullResourceAccess
        )
        );
        assertTrue(requestWithFiles.uploadedFiles().isEmpty());
    }

    @Test
    void testRequestWithNonFilesFields() {
        MockSlingJakartaHttpServletRequest slingRequest = context.jakartaRequest();
        slingRequest.addRequestParameter("namus-paremetrus", "valus-parametrus");
        WrappedSlingRequest requestWithFiles = new SlingRequest(
            slingRequest, new UserResourceAccess(
            new AuthIDUser(slingRequest.getResourceResolver().getUserID()), fullResourceAccess
        )
        );
        assertAll(
            () -> assertTrue(requestWithFiles.uploadedFiles().isEmpty()),
            () -> assertEquals("valus-parametrus", slingRequest.getParameter("namus-paremetrus"))
        );
    }

    @SneakyThrows
    @Test
    void testRequestWithFileFields() {
        MockSlingJakartaHttpServletRequest slingRequest = context.jakartaRequest();
        slingRequest.addRequestParameter("namus-paremetrus", "valus-parametrus");
        slingRequest.addRequestParameter(
            "firstImage", Files.readAllBytes(loadResourceIntoFile("1.jpeg").toPath()), MediaType.WILDCARD, "1.jpeg"
        );
        slingRequest.addRequestParameter(
            "secondImage", Files.readAllBytes(loadResourceIntoFile("2.jpeg").toPath()), MediaType.WILDCARD, "2.jpeg"
        );
        WrappedSlingRequest requestWithFiles = new SlingRequest(
            slingRequest, new UserResourceAccess(
            new AuthIDUser(slingRequest.getResourceResolver().getUserID()), fullResourceAccess
        )
        );
        assertAll(
            () -> assertEquals(2, requestWithFiles.uploadedFiles().size()),
            () -> assertEquals(
                Set.of("1.jpeg", "2.jpeg"),
                requestWithFiles.uploadedFiles()
                    .stream()
                    .map(FileWithOriginalName::originalName)
                    .collect(Collectors.toUnmodifiableSet())
            ),
            () -> assertEquals("valus-parametrus", slingRequest.getParameter("namus-paremetrus"))
        );
    }

    @Test
    void noRequestPathInfo() {
        MockSlingJakartaHttpServletRequest mockedSlingRequest = context.jakartaRequest();
        SlingRequest slingRequest = new SlingRequest(
            mockedSlingRequest, new UserResourceAccess(
            new AuthIDUser(slingHttpServletRequest().getResourceResolver().getUserID()), fullResourceAccess
        )
        );
        assertAll(
            () -> assertTrue(slingRequest.firstSelector().isEmpty()),
            () -> assertTrue(slingRequest.secondSelector().isEmpty()),
            () -> assertTrue(slingRequest.selectorString().isEmpty()),
            () -> assertEquals(NumberUtils.INTEGER_ZERO, slingRequest.numOfSelectors()),
            () -> assertTrue(slingRequest.extension().isEmpty())
        );
    }

    @SuppressWarnings(
        {
            "IllegalCatch", "ThrowCaughtLocally", "PMD.AvoidThrowingRawExceptionTypes", "PMD.ExceptionAsFlowControl",
            "PMD.AvoidCatchingGenericException"
        }
    )
    @SuppressFBWarnings("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION")
    private StackTraceElement[] stackTrace() {
        try {
            throw new RuntimeException("For stack trace");
        } catch (RuntimeException exception) {
            return exception.getStackTrace();
        }
    }

    @SuppressWarnings("MagicNumber")
    private SlingJakartaHttpServletRequest slingHttpServletRequest() {
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath("/content");
        mockRequestPathInfo.setSelectorString("delete.file-id-00313");
        mockRequestPathInfo.setExtension("mp4");
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteAddr("127.0.0.1");
        request.setRemoteHost("127.0.0.1");
        request.setRemotePort(50_261);
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_GET);
        request.setContent(new byte[]{});
        request.addHeader("namus", "valus");
        request.addHeader("namus", "gorgus");
        request.addHeader("duos", "polus");
        return request;
    }
}
