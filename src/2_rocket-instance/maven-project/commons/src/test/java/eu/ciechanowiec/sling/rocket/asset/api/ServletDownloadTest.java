package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.privilege.PrivilegeAdmin;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServletDownloadTest extends TestEnvironment {

    private ServletDownload servletDownload;
    private File file;

    ServletDownloadTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        context.registerInjectActivateService(AssetsAPI.class);
        servletDownload = context.registerInjectActivateService(ServletDownload.class);
        file = loadResourceIntoFile("1.jpeg");
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    @SneakyThrows
    @Test
    void adminAccess() {
        TargetJCRPath assetPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset asset = new StagedAssetReal(() -> Optional.of(file), new FileMetadata(file), fullResourceAccess).save(
                assetPath
        );
        assertTrue(asset.assetFile().retrieve().isPresent());
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("%s.%s".formatted(ServletDownload.SELECTOR, asset.jcrUUID()));
        mockRequestPathInfo.setExtension(
                asset.assetMetadata().filenameExtension().orElseThrow().replaceFirst("\\.", StringUtils.EMPTY)
        );
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDownload.doGet(request, response);
        assertEquals(asset.assetFile().size().bytes(), response.getContentLength());
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    @SneakyThrows
    @Test
    void userWithNoAnyAccess() {
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        TargetJCRPath assetPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset asset = new StagedAssetReal(() -> Optional.of(file), new FileMetadata(file), fullResourceAccess).save(
                assetPath
        );
        assertTrue(asset.assetFile().retrieve().isPresent());
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        doAnswer(invocation -> getRRForUser(testUser)).when(request).getResourceResolver();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("%s.%s".formatted(ServletDownload.SELECTOR, asset.jcrUUID()));
        mockRequestPathInfo.setExtension(
                asset.assetMetadata().filenameExtension().orElseThrow().replaceFirst("\\.", StringUtils.EMPTY)
        );
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDownload.doGet(request, response);
        assertAll(
                () -> assertTrue(
                        response.getOutputAsString().matches("\\{\"status\":\\{\"code\":404,\"message\":\"No asset found: '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg'\"},\"affected\":\\[\\]}")
                ),
                () -> assertTrue(asset.assetFile().retrieve().isPresent())
        );
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void userWithRequiredPrivileges() {
        TargetJCRPath assetPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset asset = new StagedAssetReal(() -> Optional.of(file), new FileMetadata(file), fullResourceAccess).save(
                assetPath
        );
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        UserResourceAccess userResourceAccess = new UserResourceAccess(testUser, fullResourceAccess);
        assertAll(
                () -> assertTrue(asset.assetFile().retrieve().isPresent()),
                () -> assertTrue(new AssetsRepository(userResourceAccess).find(asset).isEmpty())
        );
        servletDownload.requiredPrivileges().forEach(
                privilege -> new PrivilegeAdmin(fullResourceAccess).allow(
                        new TargetJCRPath("/"), testUser, privilege
                )
        );
        assertTrue(new AssetsRepository(userResourceAccess).find(asset).isPresent());
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        doAnswer(invocation -> getRRForUser(testUser)).when(request).getResourceResolver();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("%s.%s".formatted(ServletDownload.SELECTOR, asset.jcrUUID()));
        mockRequestPathInfo.setExtension(
                asset.assetMetadata().filenameExtension().orElseThrow().replaceFirst("\\.", StringUtils.EMPTY)
        );
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDownload.doGet(request, response);
        assertEquals(asset.assetFile().size().bytes(), response.getContentLength());
    }

    @SuppressWarnings("VariableDeclarationUsageDistance")
    @SneakyThrows
    @Test
    void invalidStructure() {
        TargetJCRPath assetPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset asset = new StagedAssetReal(() -> Optional.of(file), new FileMetadata(file), fullResourceAccess).save(
                assetPath
        );
        assertTrue(asset.assetFile().retrieve().isPresent());
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("%s.%s.redundant".formatted(ServletDownload.SELECTOR, asset.jcrUUID()));
        mockRequestPathInfo.setExtension(
                asset.assetMetadata().filenameExtension().orElseThrow().replaceFirst("\\.", StringUtils.EMPTY)
        );
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDownload.doGet(request, response);
        assertEquals(
                "{\"status\":{\"code\":400,\"message\":\"Invalid request structure\"},\"affected\":[]}",
                response.getOutputAsString()
        );
        assertTrue(asset.assetFile().retrieve().isPresent());
    }
}
