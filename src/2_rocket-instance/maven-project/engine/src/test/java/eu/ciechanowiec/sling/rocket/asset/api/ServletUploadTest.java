package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.privilege.PrivilegeAdmin;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SuppressWarnings(
    {"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals", "PMD.AvoidUsingHardCodedIP"}
)
class ServletUploadTest extends TestEnvironment {

    private ServletUpload servletUpload;

    ServletUploadTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        context.registerInjectActivateService(AssetsAPI.class);
        context.registerInjectActivateService(DownloadLink.class);
        servletUpload = context.registerInjectActivateService(ServletUpload.class);
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    @SneakyThrows
    @Test
    void adminAccess() {
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        MockSlingJakartaHttpServletResponse response = context.jakartaResponse();
        request.addRequestParameter(
            "firstImage", Files.readAllBytes(loadResourceIntoFile("1.jpeg").toPath()), MediaType.WILDCARD, "1.jpeg"
        );
        request.addRequestParameter(
            "secondImage", Files.readAllBytes(loadResourceIntoFile("2.jpeg").toPath()), MediaType.WILDCARD, "2.jpeg"
        );
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setExtension(ServletUpload.EXTENSION);
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setRemoteAddr("127.0.0.1");
        request.setRemoteHost("127.0.0.1");
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletUpload.doPost(request, response);
        assertTrue(
            response.getOutputAsString().matches(
                "\\{\"status\":\\{\"code\":201,\"message\":\"File\\(s\\) uploaded\"},"
                    + "\"affected\":\\[\\{\"originalName\":\"[^\"]+\","
                    + "\"assetDownloadLink\":\"http://localhost:8080/api/assets\\.download\\"
                    + ".[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg\","
                    + "\"assetDescriptor\":\"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg\"}(,"
                    + "\\{\"originalName\":\"[^\"]+\",\"assetDownloadLink\":\"http://localhost:8080/api/assets\\"
                    + ".download\\.[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg\","
                    + "\"assetDescriptor\":\"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg\"})*]}"
            )
        );
    }

    @SuppressWarnings("VariableDeclarationUsageDistance")
    @SneakyThrows
    @Test
    void userWithNoAnyAccess() {
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        MockSlingJakartaHttpServletResponse response = context.jakartaResponse();
        request.addRequestParameter(
            "firstImage", Files.readAllBytes(loadResourceIntoFile("1.jpeg").toPath()), MediaType.WILDCARD, "1.jpeg"
        );
        request.addRequestParameter(
            "secondImage", Files.readAllBytes(loadResourceIntoFile("2.jpeg").toPath()), MediaType.WILDCARD, "2.jpeg"
        );
        doAnswer(invocation -> getRRForUser(testUser)).when(request).getResourceResolver();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(getRRForUser(testUser));
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setExtension(ServletUpload.EXTENSION);
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(testUser.get());
        request.setMethod(HttpConstants.METHOD_POST);
        servletUpload.doPost(request, response);
        String expectedOutput = "{\"status\":{\"code\":400,\"message\":\"No files uploaded\"},\"affected\":[]}";
        assertEquals(expectedOutput, response.getOutputAsString());
    }

    @Test
    @SneakyThrows
    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    void userWithOnlyReadAccess() {
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        new PrivilegeAdmin(fullResourceAccess).allow(new TargetJCRPath("/"), testUser, PrivilegeConstants.JCR_READ);
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        MockSlingJakartaHttpServletResponse response = context.jakartaResponse();
        request.addRequestParameter(
            "firstImage", Files.readAllBytes(loadResourceIntoFile("1.jpeg").toPath()), MediaType.WILDCARD, "1.jpeg"
        );
        request.addRequestParameter(
            "secondImage", Files.readAllBytes(loadResourceIntoFile("2.jpeg").toPath()), MediaType.WILDCARD, "2.jpeg"
        );
        doAnswer(invocation -> getRRForUser(testUser)).when(request).getResourceResolver();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(getRRForUser(testUser));
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setExtension(ServletUpload.EXTENSION);
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(testUser.get());
        request.setMethod(HttpConstants.METHOD_POST);
        servletUpload.doPost(request, response);
        String expectedOutput = "{\"status\":{\"code\":400,\"message\":\"No files uploaded\"},\"affected\":[]}";
        assertEquals(expectedOutput, response.getOutputAsString());
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    @SneakyThrows
    @Test
    void userWithWriteAccess() {
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        PrivilegeAdmin privilegeAdmin = new PrivilegeAdmin(fullResourceAccess);
        servletUpload.requiredPrivileges().forEach(
            privilege -> privilegeAdmin.allow(new TargetJCRPath("/"), testUser, privilege)
        );
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        MockSlingJakartaHttpServletResponse response = context.jakartaResponse();
        request.addRequestParameter(
            "firstImage", Files.readAllBytes(loadResourceIntoFile("1.jpeg").toPath()), MediaType.WILDCARD, "1.jpeg"
        );
        request.addRequestParameter(
            "secondImage", Files.readAllBytes(loadResourceIntoFile("2.jpeg").toPath()), MediaType.WILDCARD, "2.jpeg"
        );
        doAnswer(invocation -> getRRForUser(testUser)).when(request).getResourceResolver();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(getRRForUser(testUser));
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setExtension(ServletUpload.EXTENSION);
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setRemoteAddr("127.0.0.1");
        request.setRemoteHost("127.0.0.1");
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(testUser.get());
        request.setMethod(HttpConstants.METHOD_POST);
        servletUpload.doPost(request, response);
        assertTrue(
            response.getOutputAsString().matches(
                "\\{\"status\":\\{\"code\":201,\"message\":\"File\\(s\\) uploaded\"},"
                    + "\"affected\":\\[\\{\"originalName\":\"[^\"]+\","
                    + "\"assetDownloadLink\":\"http://localhost:8080/api/assets\\.download\\"
                    + ".[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg\","
                    + "\"assetDescriptor\":\"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg\"}(,"
                    + "\\{\"originalName\":\"[^\"]+\",\"assetDownloadLink\":\"http://localhost:8080/api/assets\\"
                    + ".download\\.[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg\","
                    + "\"assetDescriptor\":\"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg\"})*]}"
            )
        );
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    @SneakyThrows
    @Test
    void invalidStructure() {
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        MockSlingJakartaHttpServletResponse response = context.jakartaResponse();
        request.addRequestParameter(
            "firstImage", Files.readAllBytes(loadResourceIntoFile("1.jpeg").toPath()), MediaType.WILDCARD, "1.jpeg"
        );
        request.addRequestParameter(
            "secondImage", Files.readAllBytes(loadResourceIntoFile("2.jpeg").toPath()), MediaType.WILDCARD, "2.jpeg"
        );
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("redundant");
        mockRequestPathInfo.setExtension(ServletUpload.EXTENSION);
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletUpload.doPost(request, response);
        String expectedOutput = "{\"status\":{\"code\":400,\"message\":\"Invalid request structure\"},\"affected\":[]}";
        assertEquals(expectedOutput, response.getOutputAsString());
    }
}
