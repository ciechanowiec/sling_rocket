package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("MultipleStringLiterals")
class DeletableResourceTest extends TestEnvironment {

    DeletableResourceTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustDelete() {
        File file = loadResourceIntoFile("1.jpeg");
        Asset asset = new StagedAssetReal(() -> Optional.of(file), new FileMetadata(file), resourceAccess)
                .save(new TargetJCRPath("/content/jpg"));
        String jcrUUID = asset.jcrUUID();
        AssetsRepository assetsRepository = Optional.ofNullable(context.getService(AssetsRepository.class))
                .orElseThrow();
        assertAll(
                () -> assertEquals(1, assetsRepository.all().size()),
                () -> assertTrue(() -> {
                    try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                        return Optional.ofNullable(resourceResolver.getResource("/content/jpg")).isPresent();
                    }
                }),
                () -> assertTrue(assetsRepository.find(asset).isPresent()),
                () -> assertTrue(assetsRepository.find((Referencable) () -> jcrUUID).isPresent())
        );
        Optional<JCRPath> firstDeletedJCR = new DeletableResource(asset, resourceAccess).delete();
        Optional<JCRPath> notDeletedJCROne = new DeletableResource(asset, resourceAccess).delete();
        Optional<JCRPath> notDeletedJCRTwo = new DeletableResource(
                new TargetJCRPath("/non-existent-path"), resourceAccess
        ).delete();
        assertAll(
                () -> assertTrue(assetsRepository.all().isEmpty()),
                () -> assertTrue(() -> {
                    try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                        return Optional.ofNullable(resourceResolver.getResource("/content/jpg")).isEmpty();
                    }
                }),
                () -> assertTrue(assetsRepository.find((Referencable) () -> jcrUUID).isEmpty()),
                () -> assertEquals("/content/jpg", firstDeletedJCR.orElseThrow().get()),
                () -> assertTrue(notDeletedJCROne.isEmpty()),
                () -> assertTrue(notDeletedJCRTwo.isEmpty())
        );
    }
}
