package eu.ciechanowiec.sling.rocket.asset.api;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link DownloadLink}.
 */
@ObjectClassDefinition
public @interface DownloadLinkConfig {

    /**
     * Application layer protocol. Can be {@code http} or {@code https}.
     *
     * @return application layer protocol; can be {@code http} or {@code https}
     */
    @AttributeDefinition(
        name = "Application layer protocol",
        description = "Can be 'http' or 'https'",
        defaultValue = "http",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String protocol() default "http";

    /**
     * Hostname of the server which will be addressed by the download link.
     *
     * @return hostname of the server which will be addressed by the download link
     */
    @AttributeDefinition(
        name = "Hostname",
        description = "Hostname of the server which will be addressed by the download link",
        defaultValue = "localhost",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String hostname() default "localhost";

    /**
     * If {@code true}, the port will be included in the download link. Otherwise, no port will be included, even if it
     * is specified in {@link DownloadLinkConfig#port()}.
     *
     * @return {@code true} if the port should be included in the download link; {@code false} otherwise
     */
    @AttributeDefinition(
        name = "Include port?",
        description = "If 'true', the port will be included in the download link. "
            + "Otherwise, no port will be included",
        defaultValue = "true",
        type = AttributeType.BOOLEAN
    )
    @SuppressWarnings("squid:S100")
    boolean include$_$port() default true;

    /**
     * Port which will be addressed by the download link. It will be included in the link only if
     * {@link DownloadLinkConfig#include$_$port()} is {@code true}.
     *
     * @return port which should be addressed by the download link
     */
    @AttributeDefinition(
        name = "Port",
        description = "Port which will be addressed by the download link",
        defaultValue = "8080",
        type = AttributeType.INTEGER
    )
    @SuppressWarnings({"squid:S100", "MagicNumber"})
    int port() default 8080;

    /**
     * Path to the {@link AssetsAPI} endpoint.
     *
     * @return path to the {@link AssetsAPI} endpoint
     */
    @AttributeDefinition(
        name = "Assets API Path",
        description = "The path to the Assets API endpoint",
        defaultValue = AssetsAPI.ASSETS_API_PATH,
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String assets$_$api_path() default AssetsAPI.ASSETS_API_PATH;
}
