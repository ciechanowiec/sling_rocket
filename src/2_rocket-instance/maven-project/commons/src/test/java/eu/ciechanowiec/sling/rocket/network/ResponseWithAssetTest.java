package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.asset.*;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

class ResponseWithAssetTest extends TestEnvironment {

    ResponseWithAssetTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustSend() {
        File file = loadResourceIntoFile("time-forward.mp3");
        Asset asset = new StagedAssetReal(
            new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess
        ).save(new TargetJCRPath("/content/song"));
        Asset retrievedAsset = new AssetsRepository(fullResourceAccess).find(asset).orElseThrow();
        MockSlingJakartaHttpServletResponse slingResponse = new MockSlingJakartaHttpServletResponse();
        ResponseWithAsset responseWithAsset = new ResponseWithAsset(slingResponse, retrievedAsset);
        responseWithAsset.send(ContentDispositionHeader.ATTACHMENT);
        assertAll(
            () -> assertEquals(MediaType.APPLICATION_OCTET_STREAM, slingResponse.getContentType()),
            () -> assertTrue(slingResponse.getHeader(
                HttpHeaders.CONTENT_DISPOSITION
            ).matches("attachment;filename=\".+\\.mpga\"")),
            () -> assertEquals(file.length(), slingResponse.getContentLength()),
            () -> assertEquals(file.length(), slingResponse.getOutput().length),
            () -> assertThrows(
                AlreadySentException.class, () -> responseWithAsset.send(ContentDispositionHeader.INLINE)
            )
        );
    }

    @SneakyThrows
    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void dontSendIfAlreadySent() {
        File file = loadResourceIntoFile("time-forward.mp3");
        Asset asset = new StagedAssetReal(
            new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess
        ).save(new TargetJCRPath("/content/song"));
        Asset retrievedAsset = new AssetsRepository(fullResourceAccess).find(asset).orElseThrow();
        MockSlingJakartaHttpServletResponse slingResponse = new MockSlingJakartaHttpServletResponse();
        ResponseWithAsset responseWithAsset = new ResponseWithAsset(slingResponse, retrievedAsset);
        try (PrintWriter responseWriter = slingResponse.getWriter()) {
            responseWriter.write("Some content");
            responseWriter.flush();
        }
        slingResponse.flushBuffer();
        assertAll(
            () -> assertEquals("Some content", slingResponse.getOutputAsString()),
            () -> assertTrue(slingResponse.isCommitted()),
            () -> assertThrows(
                AlreadySentException.class, () -> responseWithAsset.send(ContentDispositionHeader.INLINE)
            )
        );
    }
}
