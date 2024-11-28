package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.net.URI;

/**
 * Generates download links for Assets.
 */
@Component(
        service = DownloadLink.class,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@Designate(
        ocd = DownloadLinkConfig.class
)
@Slf4j
@ToString
@ServiceDescription("Generates download links for Assets")
public class DownloadLink {

    /**
     * {@link DownloadLinkConfig} for this {@link DownloadLink}.
     */
    private DownloadLinkConfig config;

    /**
     * Constructs an instance of this class.
     * @param config {@link DownloadLinkConfig} that will be used by the constructed object
     */
    @Activate
    public DownloadLink(DownloadLinkConfig config) {
        this.config = config;
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(DownloadLinkConfig config) {
        this.config = config;
        log.info("Configured {}", this);
    }

    @SneakyThrows
    private URI uri() {
        return new URI(
                config.protocol(),
                null,
                config.hostname(),
                config.include$_$port() ? config.port() : -1,
                config.assets$_$api_path(),
                null,
                null
        );
    }

    /**
     * Generates a download link for the specified {@link Asset}.
     * @param asset {@link Asset} for which the download link should be generated
     * @return download link for the specified {@link Asset}
     */
    public String generate(Asset asset) {
        log.trace("{} generates link for this asset: '{}'", this, asset);
        String link = "%s.%s.%s".formatted(uri(), ServletDownload.SELECTOR, new AssetDescriptor(asset));
        log.debug("For '{}' this link was generated: '{}'", asset, link);
        return link;
    }
}
