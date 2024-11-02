package eu.ciechanowiec.sling.rocket.test;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.asset.AssetsRepository;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Test environment for Sling Rocket applications. It is supposed to be used as a superclass for other test classes.
 */
@SuppressWarnings({
        "NewClassNamingConvention", "ProtectedField", "WeakerAccess", "AbstractClassName", "squid:S118",
        "VisibilityModifier", "PMD.AvoidAccessibilityAlteration", "PMD.AbstractClassWithoutAbstractMethod",
        "squid:S1694", "TestInProductSource"})
@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
@Slf4j
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
        log.debug("Initializing {} with {}", TestEnvironment.class.getSimpleName(), resourceResolverType);
        context = new SlingContext(resourceResolverType);
        context.resourceResolver(); // trigger RR initialization
        ResourceAccess resourceAccessToRegister = new ResourceAccess() {
            @Override
            public String toString() {
                return String.format("{TEST-PURPOSE %s}", ResourceAccess.class.getName());
            }

            @Override
            public ResourceResolver acquireAccess() {
                return getFreshAdminRR();
            }
        };
        resourceAccess = context.registerService(
                ResourceAccess.class, resourceAccessToRegister
        );
        context.registerInjectActivateService(AssetsRepository.class);
        log.debug("Registered {}", resourceAccess);
        boolean isRealOak = resourceResolverType == ResourceResolverType.JCR_OAK;
        Conditional.onTrueExecute(isRealOak, this::registerNodeTypes);
    }

    @SneakyThrows
    @SuppressWarnings("squid:S3011")
    private ResourceResolver getFreshAdminRR() {
        log.trace("Getting fresh admin resource resolver");
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
     * Dumps the underlying {@link Repository} into a file named "repo.xml".
     * If the file already exists, it will be overwritten.
     */
    @SneakyThrows
    @SuppressWarnings({"PMD.CloseResource", "unused"})
    protected void exportJCRtoXML() {
        try (ResourceResolver resolver = resourceAccess.acquireAccess()) {
            log.debug("Exporting JCR to XML");
            Session session = Optional.ofNullable(resolver.adaptTo(Session.class)).orElseThrow();
            Path path = Paths.get("repo.xml");
            OutputStream out = Files.newOutputStream(path);
            session.exportDocumentView("/", out, true, false);
        }
    }

    @SneakyThrows
    @SuppressWarnings({"squid:S1905", "unchecked", "rawtypes"})
    private void registerNodeTypes() {
        CNDSource cndSource = new CNDSource();
        try (InputStreamReader cndISR = cndSource.get();
             ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Session session = Optional.ofNullable(resourceResolver.adaptTo(Session.class)).orElseThrow();
            CndImporter.registerNodeTypes(cndISR, session);
            session.save();
        }
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            NodeTypeManager nodeTypeManager = Optional.ofNullable(resourceResolver.adaptTo(Session.class))
                    .orElseThrow()
                    .getWorkspace()
                    .getNodeTypeManager();
            NodeTypeIterator nodeTypesIterator = nodeTypeManager.getAllNodeTypes();
            List<NodeType> nodeTypesList = (List<NodeType>) new UnwrappedIteration<>(
                    nodeTypesIterator
            ).list();
            log.debug("Registered node types: {}", nodeTypesList);
        }
    }
}
