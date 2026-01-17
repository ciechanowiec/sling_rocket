package eu.ciechanowiec.sling.rocket.auth.reqs;

import lombok.extern.slf4j.Slf4j;
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.auth.core.impl.AuthenticationRequirementsManager;
import org.apache.sling.auth.core.impl.SlingAuthenticator;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Arrays;

/**
 * Vessel for the {@link AuthConstants#AUTH_REQUIREMENTS} OSGi {@link Component#property()}.
 * <p>
 * The sole purpose of the {@link AuthRequirementsExtension} is to be registered in the OSGi service registry with a
 * {@link AuthConstants#AUTH_REQUIREMENTS} OSGi {@link Component#property()}, which is automatically picked up by
 * {@link AuthenticationRequirementsManager} and {@link SlingAuthenticator} to dynamically extend the value of the
 * {@link SlingAuthenticator.Config#sling_auth_requirements()} OSGi {@link Component#property()}.
 */
@Component(
    immediate = true,
    service = AuthRequirementsExtension.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(
    ocd = AuthRequirementsExtensionConfig.class
)
@Slf4j
@ServiceDescription("Vessel for the 'sling.auth.requirements' OSGi property")
public class AuthRequirementsExtension {

    /**
     * Constructs an instance of this class.
     *
     * @param config {@link AuthRequirementsExtensionConfig} to configure this {@link AuthRequirementsExtension}
     */
    @Activate
    public AuthRequirementsExtension(
        AuthRequirementsExtensionConfig config
    ) {
        log.info("Initialized with {}", Arrays.toString(config.sling_auth_requirements()));
    }

    @Modified
    void configure(
        AuthRequirementsExtensionConfig config
    ) {
        log.info("Configured with {}", Arrays.toString(config.sling_auth_requirements()));
    }
}
