package eu.ciechanowiec.sling.rocket.auth.reqs;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AuthRequirementsExtensionTest extends TestEnvironment {

    AuthRequirementsExtensionTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @Test
    void mustRegisterWithOSGiProperty() {
        context.registerInjectActivateService(
            AuthRequirementsExtension.class,
            Map.of(
                AuthConstants.AUTH_REQUIREMENTS, new String[]{"-/public", "+/private/api"}
            )
        );
        BundleContext bundleContext = context.bundleContext();
        String[] actualPaths = Optional.ofNullable(
                bundleContext.getServiceReference(AuthRequirementsExtension.class)
            ).map(ServiceReference::getProperties)
            .map(props -> props.get(AuthConstants.AUTH_REQUIREMENTS))
            .map(String[].class::cast)
            .orElseThrow();
        assertArrayEquals(new String[]{"-/public", "+/private/api"}, actualPaths);
    }
}
