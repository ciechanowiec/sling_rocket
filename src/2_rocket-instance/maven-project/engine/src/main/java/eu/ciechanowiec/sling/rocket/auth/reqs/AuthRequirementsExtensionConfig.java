package eu.ciechanowiec.sling.rocket.auth.reqs;

import org.apache.sling.auth.core.AuthConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link AuthRequirementsExtension}.
 */
@ObjectClassDefinition
public @interface AuthRequirementsExtensionConfig {

    /**
     * Returns the value of the {@link AuthConstants#AUTH_REQUIREMENTS} OSGi {@link Component#property()} to be used by
     * the {@link AuthRequirementsExtension} for authentication extension.
     *
     * @return value of the {@link AuthConstants#AUTH_REQUIREMENTS} OSGi {@link Component#property()} to be used by the
     * {@link AuthRequirementsExtension} for authentication extension
     */
    @AttributeDefinition(
        name = "Sling Auth Requirements",
        description = "Value of the 'sling.auth.requirements' OSGi property to be used by the "
            + "AuthRequirementsExtension for authentication extension",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String[] sling_auth_requirements();
}
