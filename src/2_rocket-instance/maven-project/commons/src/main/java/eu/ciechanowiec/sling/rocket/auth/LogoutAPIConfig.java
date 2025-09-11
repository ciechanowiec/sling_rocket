package eu.ciechanowiec.sling.rocket.auth;

import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * {@link ObjectClassDefinition} for {@link LogoutAPI}.
 */
@ObjectClassDefinition
public @interface LogoutAPIConfig {

    /**
     * Returns {@code true} if {@link LogoutAPI} should be providing a relevant {@link Resource} for the requested path.
     * Returns {@code false} if a {@link NonExistingResource} should be always provided.
     *
     * @return {@code true} if {@link LogoutAPI} should be providing a relevant {@link Resource} for the requested path;
     * returns {@code false} if a {@link NonExistingResource} should be always provided
     */
    @AttributeDefinition(
        name = "Enabled?",
        description = "If 'true', the Logout API will be providing a relevant resource for the requested path. "
            + "Otherwise, a non-existing resource will be always provided",
        defaultValue = "true",
        type = AttributeType.BOOLEAN
    )
    @SuppressWarnings("squid:S100")
    boolean is$_$enabled() default true;
}
