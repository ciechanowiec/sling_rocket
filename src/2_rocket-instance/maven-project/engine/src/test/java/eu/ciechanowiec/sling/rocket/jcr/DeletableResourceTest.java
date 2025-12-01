package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.asset.*;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.ref.Referenceable;
import eu.ciechanowiec.sling.rocket.privilege.PrivilegeAdmin;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
class DeletableResourceTest extends TestEnvironment {

    DeletableResourceTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustDelete() {
        File file = loadResourceIntoFile("1.jpeg");
        Asset asset = new StagedAssetReal(new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess)
            .save(new TargetJCRPath("/content/jpg"));
        String jcrUUID = asset.jcrUUID();
        AssetsRepository assetsRepository = new AssetsRepository(fullResourceAccess);
        assertAll(
            () -> assertEquals(1, assetsRepository.all().size()),
            () -> assertTrue(() -> {
                try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
                    return Optional.ofNullable(resourceResolver.getResource("/content/jpg")).isPresent();
                }
            }),
            () -> assertTrue(assetsRepository.find(asset).isPresent()),
            () -> assertTrue(assetsRepository.find((Referenceable) () -> jcrUUID).isPresent())
        );
        Optional<JCRPath> firstDeletedJCR = new DeletableResource(asset, fullResourceAccess).delete();
        Optional<JCRPath> notDeletedJCROne = new DeletableResource(asset, fullResourceAccess).delete();
        Optional<JCRPath> notDeletedJCRTwo = new DeletableResource(
            new TargetJCRPath("/non-existent-path"), fullResourceAccess
        ).delete();
        assertAll(
            () -> assertTrue(assetsRepository.all().isEmpty()),
            () -> assertTrue(() -> {
                try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
                    return Optional.ofNullable(resourceResolver.getResource("/content/jpg")).isEmpty();
                }
            }),
            () -> assertTrue(assetsRepository.find((Referenceable) () -> jcrUUID).isEmpty()),
            () -> assertEquals("/content/jpg", firstDeletedJCR.orElseThrow().get()),
            () -> assertTrue(notDeletedJCROne.isEmpty()),
            () -> assertTrue(notDeletedJCRTwo.isEmpty())
        );
    }

    @Test
    void mustNotDeleteBecauseOfInsufficientAccess() {
        context.build().resource("/content").commit();
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        new PrivilegeAdmin(fullResourceAccess).allow(
            new TargetJCRPath("/content"), testUser, PrivilegeConstants.JCR_READ
        );
        File file = loadResourceIntoFile("1.jpeg");
        Asset asset = new StagedAssetReal(new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess)
            .save(new TargetJCRPath("/content/jpg"));
        String jcrUUID = asset.jcrUUID();
        UserResourceAccess userResourceAccess = new UserResourceAccess(testUser, fullResourceAccess);
        AssetsRepository assetsRepository = new AssetsRepository(userResourceAccess);
        assertAll(
            () -> assertEquals(1, assetsRepository.all().size()),
            () -> assertTrue(() -> {
                try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
                    return Optional.ofNullable(resourceResolver.getResource("/content/jpg")).isPresent();
                }
            }),
            () -> assertTrue(assetsRepository.find(asset).isPresent()),
            () -> assertTrue(assetsRepository.find((Referenceable) () -> jcrUUID).isPresent()),
            () -> assertTrue(new DeletableResource(asset, userResourceAccess).delete().isEmpty()),
            () -> assertEquals(1, assetsRepository.all().size())
        );
        new DeletableResource(asset, userResourceAccess).requiredPrivileges().forEach(
            privilege -> new PrivilegeAdmin(fullResourceAccess).allow(
                new TargetJCRPath("/content"), testUser, privilege
            )
        );
        Optional<JCRPath> firstDeletedJCR = new DeletableResource(asset, userResourceAccess).delete();
        Optional<JCRPath> notDeletedJCROne = new DeletableResource(asset, userResourceAccess).delete();
        assertAll(
            () -> assertTrue(assetsRepository.all().isEmpty()),
            () -> assertTrue(() -> {
                try (ResourceResolver resourceResolver = userResourceAccess.acquireAccess()) {
                    return Optional.ofNullable(resourceResolver.getResource("/content/jpg")).isEmpty();
                }
            }),
            () -> assertTrue(assetsRepository.find((Referenceable) () -> jcrUUID).isEmpty()),
            () -> assertEquals("/content/jpg", firstDeletedJCR.orElseThrow().get()),
            () -> assertTrue(notDeletedJCROne.isEmpty())
        );
    }
}
