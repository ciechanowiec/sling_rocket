package eu.ciechanowiec.sling.rocket.test;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.asset.AssetImplementationPicker;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import lombok.SneakyThrows;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Repository;
import javax.jcr.Session;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Test environment for Sling Rocket applications. It is supposed to be used as a superclass for other test classes.
 */
@SuppressWarnings({
        "NewClassNamingConvention", "ProtectedField", "WeakerAccess", "AbstractClassName", "squid:S118",
        "VisibilityModifier", "PMD.AvoidAccessibilityAlteration", "PMD.AbstractClassWithoutAbstractMethod",
        "squid:S1694", "TestInProductSource"})
@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public abstract class TestEnvironment {

    /**
     * The {@link SlingContext} of the test environment.
     */
    protected final SlingContext context;

    /**
     * {@link ResourceAccess} that will be used by the test environment to acquire access to resources.
     */
    protected final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     * @param resourceResolverType the type of {@link ResourceResolver} to be used for the persistence layer
     */
    @SuppressWarnings({"resource", "squid:S5993"})
    public TestEnvironment(ResourceResolverType resourceResolverType) {
        context = new SlingContext(resourceResolverType);
        context.resourceResolver(); // trigger RR initialization
        resourceAccess = this::getFreshAdminRR;
        context.registerService(ResourceAccess.class, resourceAccess);
        context.registerInjectActivateService(AssetImplementationPicker.class);
        boolean isRealOak = resourceResolverType == ResourceResolverType.JCR_OAK;
        Conditional.onTrueExecute(isRealOak, this::registerNodeTypes);
    }

    @SneakyThrows
    @SuppressWarnings("squid:S3011")
    private ResourceResolver getFreshAdminRR() {
        Class<SlingContextImpl> slingContextClass = SlingContextImpl.class;
        Field resourceResolverFactoryField = slingContextClass.getDeclaredField("resourceResolverFactory");
        resourceResolverFactoryField.setAccessible(true);
        ResourceResolverFactory resourceResolverFactory =
                (ResourceResolverFactory) resourceResolverFactoryField.get(context);
        @SuppressWarnings("deprecation")
        ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        return resourceResolver;
    }

    /**
     * Dumps the underlying {@link Repository} into a file.
     */
    @SneakyThrows
    @SuppressWarnings({"resource", "PMD.CloseResource", "unused"})
    protected void exportJCRtoXML() {
        ResourceResolver resolver = resourceAccess.acquireAccess();
        Session session = Optional.ofNullable(resolver.adaptTo(Session.class)).orElseThrow();
        Path path = Paths.get("repo.xml");
        OutputStream out = Files.newOutputStream(path);
        session.exportDocumentView("/", out, true, false);
    }

    @SneakyThrows
    private void registerNodeTypes() {
        ClassLoader classLoader = TestEnvironment.class.getClassLoader();
        InputStream cndIS = Optional.ofNullable(
                classLoader.getResourceAsStream("SLING-INF/notetypes/nodetypes.cnd")
        ).orElseThrow();
        try (InputStreamReader cndISR = new InputStreamReader(cndIS, StandardCharsets.UTF_8);
             ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Session session = Optional.ofNullable(resourceResolver.adaptTo(Session.class)).orElseThrow();
            CndImporter.registerNodeTypes(cndISR, session);
        }
    }
}
