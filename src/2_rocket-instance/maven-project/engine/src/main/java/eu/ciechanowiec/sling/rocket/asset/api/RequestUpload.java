package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.asset.UsualFileAsAssetFile;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.network.Affected;
import eu.ciechanowiec.sling.rocket.network.SlingRequest;
import eu.ciechanowiec.sling.rocket.network.SlingRequestWithDecomposition;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ToString
class RequestUpload implements SlingRequestWithDecomposition {

    private final SlingRequest slingRequest;
    private final DownloadLink downloadLink;

    RequestUpload(SlingRequest slingRequest, DownloadLink downloadLink) {
        this.slingRequest = slingRequest;
        this.downloadLink = downloadLink;
    }

    @Override
    public String contentPath() {
        return slingRequest.contentPath();
    }

    @Override
    public Optional<String> firstSelector() {
        return slingRequest.firstSelector();
    }

    @Override
    public Optional<String> secondSelector() {
        return slingRequest.secondSelector();
    }

    @Override
    public Optional<String> thirdSelector() {
        return slingRequest.thirdSelector();
    }

    @Override
    public Optional<String> selectorString() {
        return slingRequest.selectorString();
    }

    @Override
    public int numOfSelectors() {
        return slingRequest.numOfSelectors();
    }

    @Override
    public Optional<String> extension() {
        return slingRequest.extension();
    }

    boolean isValidStructure() {
        return new RequestStructure(this).isValid();
    }

    List<Affected> saveAssets(ParentJCRPath parentJCRPath, boolean doIncludeDownloadLink) {
        log.trace("{} saving assets at {}", this, parentJCRPath);
        UserResourceAccess userResourceAccess = slingRequest.userResourceAccess();
        return slingRequest.uploadedFiles()
            .stream()
            .map(
                fileWithOriginalName -> {
                    File file = fileWithOriginalName.file();
                    String originalName = fileWithOriginalName.originalName();
                    return new StagedAssetReal(
                        new UsualFileAsAssetFile(file),
                        new FileMetadata(file)
                            .set("originalName", originalName)
                            .set("remoteAddress", slingRequest.remoteAddress())
                            .set("remoteHost", slingRequest.remoteHost()),
                        userResourceAccess
                    );
                }
            )
            .map(SafeSaving::new)
            .map(safeSaving -> safeSaving.save(new TargetJCRPath(parentJCRPath, UUID.randomUUID())))
            .flatMap(Optional::stream)
            .map(
                asset -> (Affected) new AssetDescriptor(
                    asset, doIncludeDownloadLink ? downloadLink.generate(asset) : StringUtils.EMPTY
                )
            )
            .toList();
    }

    @Override
    public Optional<String> suffix() {
        return slingRequest.suffix();
    }
}
