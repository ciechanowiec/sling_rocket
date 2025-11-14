package eu.ciechanowiec.sling.rocket.asset.api;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link ServletUpload}.
 */
@ObjectClassDefinition
public @interface ServletUploadConfig {

    /**
     * Default {@link JCRPath} where all uploaded {@link Asset}s should be stored.
     */
    @SuppressWarnings("squid:S1075")
    String DEFAULT_JCR_PATH = "/content/rocket/default-assets-pool";

    /**
     * {@link JCRPath} where all uploaded {@link Asset}s should be stored.
     *
     * @return {@link JCRPath} where all uploaded {@link Asset}s should be stored
     */
    @AttributeDefinition(
        name = "JCR Path",
        description = "JCR path where all uploaded Assets should be stored. "
            + "If it doesn't exist, it will be initiated by the service",
        defaultValue = DEFAULT_JCR_PATH,
        type = AttributeType.STRING
    )
    String jcr_path() default DEFAULT_JCR_PATH;

    /**
     * If {@code true}, the download link for every uploaded {@link Asset} will be included in the response. Otherwise,
     * no download link will be included.
     *
     * @return {@code true} if the download link for every uploaded {@link Asset} should be included in the response;
     * {@code false} otherwise
     */
    @AttributeDefinition(
        name = "Do include download link",
        description = "If 'true', the download link for every uploaded Asset will be included in the response. "
            + "Otherwise, no download link will be included",
        defaultValue = "true",
        type = AttributeType.BOOLEAN
    )
    boolean do$_$include$_$download$_$link() default true;
}
