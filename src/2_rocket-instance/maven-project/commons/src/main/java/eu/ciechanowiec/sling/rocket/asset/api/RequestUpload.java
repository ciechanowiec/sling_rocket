package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.network.Affected;
import eu.ciechanowiec.sling.rocket.network.Request;
import eu.ciechanowiec.sling.rocket.network.RequestWithDecomposition;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ToString
class RequestUpload implements RequestWithDecomposition {

    private final Request request;
    private final DownloadLink downloadLink;

    RequestUpload(Request request, DownloadLink downloadLink) {
        this.request = request;
        this.downloadLink = downloadLink;
    }

    @Override
    public String contentPath() {
        return request.contentPath();
    }

    @Override
    public Optional<String> firstSelector() {
        return request.firstSelector();
    }

    @Override
    public Optional<String> secondSelector() {
        return request.secondSelector();
    }

    @Override
    public Optional<String> thirdSelector() {
        return request.thirdSelector();
    }

    @Override
    public Optional<String> selectorString() {
        return request.selectorString();
    }

    @Override
    public int numOfSelectors() {
        return request.numOfSelectors();
    }

    @Override
    public Optional<String> extension() {
        return request.extension();
    }

    boolean isValidStructure() {
        return new RequestStructure(this).isValid();
    }

    @SuppressWarnings("PMD.UnnecessaryCast")
    List<Affected> saveAssets(ParentJCRPath parentJCRPath, boolean doIncludeDownloadLink) {
        log.trace("{} saving assets at {}", this, parentJCRPath);
        UserResourceAccess userResourceAccess = request.userResourceAccess();
        return request.uploadedFiles()
                .stream()
                .map(fileWithOriginalName -> {
                    File file = fileWithOriginalName.file();
                    String originalName = fileWithOriginalName.originalName();
                    return new StagedAssetReal(
                            () -> Optional.of(file),
                            new FileMetadata(file)
                                    .set("originalName", originalName)
                                    .set("remoteAddress", request.remoteAddress())
                                    .set("remoteHost", request.remoteHost()),
                            userResourceAccess
                    );
                })
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
}
