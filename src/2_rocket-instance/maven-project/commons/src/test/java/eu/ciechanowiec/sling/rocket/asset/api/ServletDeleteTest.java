package eu.ciechanowiec.sling.rocket.asset.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetFile;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.asset.UsualFileAsAssetFile;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.privilege.PrivilegeAdmin;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import java.io.File;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyStaticImports"})
class ServletDeleteTest extends TestEnvironment {

    private ServletDelete servletDelete;
    private File file;

    ServletDeleteTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        context.registerInjectActivateService(AssetsAPI.class);
        servletDelete = context.registerInjectActivateService(ServletDelete.class);
        file = loadResourceIntoFile("1.jpeg");
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    @SneakyThrows
    @Test
    void adminAccess() {
        TargetJCRPath assetPath = new TargetJCRPath(
            new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset asset = new StagedAssetReal(
            new UsualFileAsAssetFile(file),
            new FileMetadata(file).set(AssetFile.PN_ORIGINAL_NAME, "some original name"),
            fullResourceAccess
        ).save(assetPath);
        try (InputStream is = asset.assetFile().retrieve()) {
            assertTrue(is.readAllBytes().length > NumberUtils.INTEGER_ZERO);
        }
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("%s.%s".formatted(ServletDelete.SELECTOR, asset.jcrUUID()));
        mockRequestPathInfo.setExtension(
            asset.assetMetadata().filenameExtension().orElseThrow().replaceFirst("\\.", StringUtils.EMPTY)
        );
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDelete.doPost(request, response);
        assertTrue(
            response.getOutputAsString().matches(
                "\\{\"status\":\\{\"code\":200,\"message\":\"Asset deleted\"},"
                    + "\"affected\":\\[\\{\"assetDescriptor\":\"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0"
                    + "-9]{12}\\.jpg\"}]}"
            )
        );
        try (InputStream is = asset.assetFile().retrieve()) {
            assertEquals((int) NumberUtils.INTEGER_ZERO, is.readAllBytes().length);
        }
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    @SneakyThrows
    @Test
    void userWithNoAnyAccess() {
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        TargetJCRPath assetPath = new TargetJCRPath(
            new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset asset = new StagedAssetReal(
            new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess).save(
            assetPath
        );
        try (InputStream is = asset.assetFile().retrieve()) {
            assertTrue(is.readAllBytes().length > NumberUtils.INTEGER_ZERO);
        }
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        doAnswer(invocation -> getRRForUser(testUser)).when(request).getResourceResolver();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("%s.%s".formatted(ServletDelete.SELECTOR, asset.jcrUUID()));
        mockRequestPathInfo.setExtension(
            asset.assetMetadata().filenameExtension().orElseThrow().replaceFirst("\\.", StringUtils.EMPTY)
        );
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDelete.doPost(request, response);
        assertAll(
            () -> assertTrue(
                response.getOutputAsString().matches(
                    "\\{\"status\":\\{\"code\":400,\"message\":\"Unable to delete: "
                        + "'[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg'\"},"
                        + "\"affected\":\\[\\]}")
            ),
            () -> {
                try (InputStream is = asset.assetFile().retrieve()) {
                    assertTrue(is.readAllBytes().length > NumberUtils.INTEGER_ZERO);
                }
            }
        );
    }

    @Test
    @SneakyThrows
    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    void userWithOnlyReadAccess() {
        TargetJCRPath assetPath = new TargetJCRPath(
            new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset asset = new StagedAssetReal(
            new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess).save(
            assetPath
        );
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        UserResourceAccess userResourceAccess = new UserResourceAccess(testUser, fullResourceAccess);
        assertAll(
            () -> {
                try (InputStream is = asset.assetFile().retrieve()) {
                    assertTrue(is.readAllBytes().length > NumberUtils.INTEGER_ZERO);
                }
            },
            () -> assertTrue(new AssetsRepository(userResourceAccess).find(asset).isEmpty())
        );
        new PrivilegeAdmin(fullResourceAccess).allow(new TargetJCRPath("/"), testUser, PrivilegeConstants.JCR_READ);
        assertTrue(new AssetsRepository(userResourceAccess).find(asset).isPresent());
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        doAnswer(invocation -> getRRForUser(testUser)).when(request).getResourceResolver();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("%s.%s".formatted(ServletDelete.SELECTOR, asset.jcrUUID()));
        mockRequestPathInfo.setExtension(
            asset.assetMetadata().filenameExtension().orElseThrow().replaceFirst("\\.", StringUtils.EMPTY)
        );
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDelete.doPost(request, response);
        assertAll(
            () -> assertTrue(
                response.getOutputAsString().matches(
                    "\\{\"status\":\\{\"code\":400,\"message\":\"Unable to delete: "
                        + "'[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg'\"},"
                        + "\"affected\":\\[\\]}")
            ),
            () -> {
                try (InputStream is = asset.assetFile().retrieve()) {
                    assertTrue(is.readAllBytes().length > NumberUtils.INTEGER_ZERO);
                }
            }
        );
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    @SneakyThrows
    @Test
    void userWithDeleteAccess() {
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        TargetJCRPath assetPath = new TargetJCRPath(
            new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset asset = new StagedAssetReal(
            new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess).save(
            assetPath
        );
        try (InputStream is = asset.assetFile().retrieve()) {
            assertTrue(is.readAllBytes().length > NumberUtils.INTEGER_ZERO);
        }
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        doAnswer(invocation -> getRRForUser(testUser)).when(request).getResourceResolver();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("%s.%s".formatted(ServletDelete.SELECTOR, asset.jcrUUID()));
        mockRequestPathInfo.setExtension(
            asset.assetMetadata().filenameExtension().orElseThrow().replaceFirst("\\.", StringUtils.EMPTY)
        );
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDelete.requiredPrivileges().forEach(
            privilege -> new PrivilegeAdmin(fullResourceAccess).allow(
                new TargetJCRPath("/content"), testUser, privilege
            )
        );
        servletDelete.doPost(request, response);
        assertAll(
            () -> assertTrue(
                response.getOutputAsString().matches(
                    "\\{\"status\":\\{\"code\":200,\"message\":\"Asset deleted\"},"
                        + "\"affected\":\\[\\{\"assetDescriptor\":\"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4"
                        + "}-[a-f0-9]{12}\\.jpg\"}]}")
            ),
            () -> {
                try (InputStream is = asset.assetFile().retrieve()) {
                    assertEquals((int) NumberUtils.INTEGER_ZERO, is.readAllBytes().length);
                }
            }
        );
    }

    @SuppressWarnings({"VariableDeclarationUsageDistance", "LineLength"})
    @SneakyThrows
    @Test
    void invalidStructure() {
        TargetJCRPath assetPath = new TargetJCRPath(
            new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset asset = new StagedAssetReal(
            new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess).save(
            assetPath
        );
        try (InputStream is = asset.assetFile().retrieve()) {
            assertTrue(is.readAllBytes().length > NumberUtils.INTEGER_ZERO);
        }
        Resource currentResource = Objects.requireNonNull(context.currentResource(AssetsAPI.ASSETS_API_PATH));
        MockSlingHttpServletRequest request = spy(context.request());
        MockSlingHttpServletResponse response = context.response();
        MockRequestPathInfo mockRequestPathInfo = new MockRequestPathInfo(context.resourceResolver());
        mockRequestPathInfo.setResourcePath(AssetsAPI.ASSETS_API_PATH);
        mockRequestPathInfo.setSelectorString("%s.%s.redundant".formatted(ServletDelete.SELECTOR, asset.jcrUUID()));
        mockRequestPathInfo.setExtension(
            asset.assetMetadata().filenameExtension().orElseThrow().replaceFirst("\\.", StringUtils.EMPTY)
        );
        lenient().when(request.getRequestPathInfo()).thenReturn(mockRequestPathInfo);
        request.setPathInfo(currentResource.getPath());
        request.setRemoteUser(MockJcr.DEFAULT_USER_ID);
        request.setMethod(HttpConstants.METHOD_POST);
        servletDelete.doPost(request, response);
        assertEquals(
            "{\"status\":{\"code\":400,\"message\":\"Invalid request structure\"},\"affected\":[]}",
            response.getOutputAsString()
        );
        try (InputStream is = asset.assetFile().retrieve()) {
            assertTrue(is.readAllBytes().length > NumberUtils.INTEGER_ZERO);
        }
    }
}
