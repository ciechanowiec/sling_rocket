package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.asset.UsualFileAsAssetFile;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AssetDescriptorTest extends TestEnvironment {

    private File file;

    AssetDescriptorTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        file = loadResourceIntoFile("1.jpeg");
    }

    @Test
    void test() {
        TargetJCRPath assetPathOne = new TargetJCRPath(
            new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        TargetJCRPath assetPathTwo = new TargetJCRPath(
            new ParentJCRPath(new TargetJCRPath("/content/images")), UUID.randomUUID()
        );
        Asset assetOne = new StagedAssetReal(
            new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess).save(
            assetPathOne
        );
        Asset assetTwo = new StagedAssetReal(
            new UsualFileAsAssetFile(file), new FileMetadata(file), fullResourceAccess).save(
            assetPathTwo
        );
        AssetDescriptor assetDescriptorOne = new AssetDescriptor(assetOne);
        AssetDescriptor assetDescriptorTwoA = new AssetDescriptor(assetTwo);
        AssetDescriptor assetDescriptorTwoB = new AssetDescriptor(assetTwo);
        assertNotEquals(assetDescriptorOne, assetDescriptorTwoA);
        assertEquals(assetDescriptorTwoA, assetDescriptorTwoB);
    }
}
