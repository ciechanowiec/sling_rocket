package eu.ciechanowiec.sling.rocket.google;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link GoogleIdTokenVerifierProxy}.
 */
@ObjectClassDefinition
public @interface GoogleIdTokenVerifierProxyConfig {

    /**
     * OAuth 2.0 audience for the Google ID token verification, which is typically the client ID.
     *
     * @return OAuth 2.0 audience for the Google ID token verification, which is typically the client ID
     */
    @AttributeDefinition(
        name = "Audience",
        description = "OAuth 2.0 audience for the Google ID token verification, which is typically the client ID",
        defaultValue = StringUtils.EMPTY,
        type = AttributeType.STRING
    )
    String audience() default StringUtils.EMPTY;
}
