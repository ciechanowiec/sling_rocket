package eu.ciechanowiec.sling.rocket.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.testing.mock.sling.loader.ContentLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"resource", "MultipleStringLiterals"})
@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
class FullResourceAccessTest {

    private final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    private ResourceAccess resourceAccess;

    @BeforeEach
    @SneakyThrows
    void setup() {
        context.resourceResolver(); // trigger RR initialization
        ServiceUserMapped serviceUserMapped = new ServiceUserMapped() {

        };
        Map<String, Object> props = Map.of(ServiceUserMapped.SUBSERVICENAME, FullResourceAccess.SUBSERVICE_NAME);
        context.registerService(ServiceUserMapped.class, serviceUserMapped, props);
        resourceAccess = context.registerInjectActivateService(FullResourceAccess.class);
        Class<FullResourceAccessTest> testClass = FullResourceAccessTest.class;
        try (InputStream testData = testClass.getResourceAsStream("BasicJCRData.json")) {
            Objects.requireNonNull(testData);
            ContentLoader contentLoader = context.load();
            contentLoader.json(testData, "/content");
        }
    }

    @SneakyThrows
    @Test
    void mustAcquireAccess() {
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Resource resource = Optional.ofNullable(resourceResolver.getResource("/content")).orElseThrow();
            ValueMap props = resource.getValueMap();
            String initialActualValue = props.get("customProperty", String.class);
            assertEquals("Customus Propertius", initialActualValue);
            ModifiableValueMap modifiableValueMap = Optional.ofNullable(resource.adaptTo(ModifiableValueMap.class))
                .orElseThrow();
            modifiableValueMap.put("customProperty", "Novus Propertius");
            resourceResolver.commit();
        }
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Resource resource = Optional.ofNullable(resourceResolver.getResource("/content")).orElseThrow();
            ValueMap props = resource.getValueMap();
            String newActualValue = props.get("customProperty", String.class);
            assertEquals("Novus Propertius", newActualValue);
        }
    }
}
