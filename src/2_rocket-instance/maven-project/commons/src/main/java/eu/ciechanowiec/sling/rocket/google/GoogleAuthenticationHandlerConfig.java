package eu.ciechanowiec.sling.rocket.google;

import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link GoogleAuthenticationHandler}.
 */
@ObjectClassDefinition
@SuppressWarnings("TypeName")
public @interface GoogleAuthenticationHandlerConfig {

    /**
     * Values for the {@link AuthenticationHandler#PATH_PROPERTY} property.
     *
     * @return values for the {@link AuthenticationHandler#PATH_PROPERTY} property
     */
    @AttributeDefinition(
        name = "Path",
        description = "Values for the AuthenticationHandler `path` property",
        defaultValue = "/",
        type = AttributeType.STRING
    )
    String[] path() default "/";
}
