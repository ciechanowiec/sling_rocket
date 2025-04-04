package eu.ciechanowiec.sling.rocket.asset.api;

import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * {@link ObjectClassDefinition} for {@link AssetsAPI}.
 */
@ObjectClassDefinition
public @interface AssetsAPIConfig {

    /**
     * Returns {@code true} if {@link AssetsAPI} should be providing a relevant {@link Resource} for the requested path.
     * Returns {@code false} if a {@link NonExistingResource} should be always provided.
     *
     * @return {@code true} if {@link AssetsAPI} should be providing a relevant {@link Resource} for the requested path;
     * returns {@code false} if a {@link NonExistingResource} should be always provided
     */
    @AttributeDefinition(
        name = "Enabled?",
        description = "If 'true', the Assets API will be providing a relevant resource for the requested path. "
            + "Otherwise, a non-existing resource will be always provided",
        defaultValue = "true",
        type = AttributeType.BOOLEAN
    )
    @SuppressWarnings("squid:S100")
    boolean is$_$enabled() default true;
}
