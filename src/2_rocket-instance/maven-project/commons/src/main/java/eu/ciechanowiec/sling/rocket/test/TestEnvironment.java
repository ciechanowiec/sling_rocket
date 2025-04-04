package eu.ciechanowiec.sling.rocket.test;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.FullResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import eu.ciechanowiec.sling.rocket.identity.AuthID;
import eu.ciechanowiec.sling.rocket.identity.AuthIDGroup;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.identity.WithUserManager;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.config.DataSourceConfig;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test environment for Sling Rocket applications. It is supposed to be used as a superclass for other test classes.
 */
@Slf4j
@SuppressWarnings(
    {
        "NewClassNamingConvention", "ProtectedField", "WeakerAccess", "AbstractClassName", "squid:S118",
        "VisibilityModifier", "PMD.AvoidAccessibilityAlteration", "PMD.AbstractClassWithoutAbstractMethod",
        "squid:S1694", "TestInProductSource", "PMD.ExcessiveImports", "ClassFanOutComplexity",
        "PMD.CouplingBetweenObjects"
    }
)
@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public abstract class TestEnvironment {

    /**
     * The {@link SlingContext} of the test environment.
     */
    protected final SlingContext context;

    /**
     * {@link FullResourceAccess} that can be used in the test environment to acquire access to resources.
     */
    protected FullResourceAccess fullResourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param resourceResolverType the type of {@link ResourceResolver} to be used for the persistence layer
     */
    @SneakyThrows
    protected TestEnvironment(ResourceResolverType resourceResolverType) {
        log.debug("Initializing {} with {}", TestEnvironment.class.getSimpleName(), resourceResolverType);
        context = new SlingContext(resourceResolverType);
    }

    @BeforeEach
    @SuppressWarnings({"resource", "squid:S5993", "squid:S2440"})
    void sharedSetup() {
        log.debug("Triggering RR initialization");
        context.resourceResolver(); // trigger RR initialization
        Map<String, Object> props = Map.of(ServiceUserMapped.SUBSERVICENAME, FullResourceAccess.SUBSERVICE_NAME);
        context.registerService(
            ServiceUserMapped.class, new ServiceUserMapped() {

            }, props
        );
        fullResourceAccess = mock(FullResourceAccess.class);
        boolean isRealOak = context.resourceResolverType() == ResourceResolverType.JCR_OAK;
        lenient().doAnswer(
            invocation -> {
                AuthIDUser passedAuthIDUser = invocation.getArgument(NumberUtils.INTEGER_ZERO);
                if (isRealOak && !passedAuthIDUser.get().equals(MockJcr.DEFAULT_USER_ID)) {
                    return getRRForUser(passedAuthIDUser);
                } else {
                    return getFreshAdminRR();
                }
            }
        ).when(fullResourceAccess).acquireAccess(any(AuthIDUser.class));
        lenient().doAnswer(invocation -> getFreshAdminRR()).when(fullResourceAccess).acquireAccess();
        context.registerInjectActivateService(fullResourceAccess);
        log.debug("Registered {}", fullResourceAccess);
        Conditional.onTrueExecute(isRealOak, this::registerNodeTypes);
    }

    /**
     * Retrieves a {@link ResourceResolver} for a {@link User} represented by the specified {@link AuthIDUser}.
     * <p>
     * The {@link User} for which the {@link ResourceResolver} is returned must be loggable with
     * {@link SimpleCredentials} and {@link DataSourceConfig#PASSWORD} as the password.
     *
     * @param authIDUser {@link AuthIDUser} for which a {@link ResourceResolver} should be returned.
     * @return {@link ResourceResolver} for a {@link User} represented by the specified {@link AuthIDUser}
     */
    @SneakyThrows
    protected ResourceResolver getRRForUser(AuthIDUser authIDUser) {
        log.trace("Getting resource resolver for this user: {}", authIDUser);
        Optional<ResourceResolverFactory> rrFactoryNullable =
            Optional.ofNullable(context.getService(ResourceResolverFactory.class));
        Optional<Repository> repositoryNullable = Optional.ofNullable(context.getService(SlingRepository.class));
        ResourceResolverFactory resourceResolverFactory = rrFactoryNullable.orElseThrow();
        Repository repository = repositoryNullable.orElseThrow();
        Credentials credentials = new SimpleCredentials(authIDUser.get(), DataSourceConfig.PASSWORD.toCharArray());
        Session userSession = repository.login(credentials);
        Map<String, Object> authInfo =
            Collections.singletonMap(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, userSession);
        return resourceResolverFactory.getResourceResolver(authInfo);
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
     * Loads a resource from the classpath and saves it to a new temporary {@link File} on the disk.
     *
     * @param resourceName name of the resource to be loaded
     * @return {@link File} that was created
     */
    @SneakyThrows
    protected File loadResourceIntoFile(String resourceName) {
        File createdFile = File.createTempFile("jcr-binary_", ".tmp");
        createdFile.deleteOnExit();
        Path tempFilePath = createdFile.toPath();
        Thread currentThread = Thread.currentThread();
        ClassLoader classLoader = currentThread.getContextClassLoader();
        try (
            InputStream inputStream = Optional.ofNullable(
                classLoader.getResourceAsStream(resourceName)
            ).orElseThrow();
            OutputStream outputStream = Files.newOutputStream(tempFilePath)
        ) {
            IOUtils.copy(inputStream, outputStream);
        }
        assertTrue(createdFile.exists());
        return createdFile;
    }

    /**
     * Dumps the underlying {@link Repository} into a file named "repo.xml". If the file already exists, it will be
     * overwritten.
     */
    @SneakyThrows
    @SuppressWarnings({"PMD.CloseResource", "unused"})
    protected void exportJCRtoXML() {
        try (ResourceResolver resolver = fullResourceAccess.acquireAccess()) {
            log.debug("Exporting JCR to XML");
            Session session = Optional.ofNullable(resolver.adaptTo(Session.class)).orElseThrow();
            Path path = Paths.get("repo.xml");
            OutputStream out = Files.newOutputStream(path);
            session.exportDocumentView("/", out, true, false);
        }
    }

    /**
     * Retrieves an {@link AuthID} of a {@link User} specified by the passed {@link AuthIDUser}. If the {@link User}
     * doesn't already exist, it is created.
     *
     * @param authIDUser {@link AuthIDUser} for the retrieved {@link User}.
     * @return {@link AuthID} for the retrieved {@link User}.
     */
    protected AuthIDUser createOrGetUser(AuthIDUser authIDUser) {
        return (AuthIDUser) createOrGetAuthorizable(authIDUser, User.class);
    }

    /**
     * Retrieves an {@link AuthID} of a {@link Group} specified by the passed {@link AuthIDGroup}. If the {@link Group}
     * doesn't already exist, it is created.
     *
     * @param authIDGroup {@link AuthIDGroup} for the retrieved {@link Group}.
     * @return {@link AuthID} for the retrieved {@link Group}.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    protected AuthIDGroup createOrGetGroup(AuthIDGroup authIDGroup) {
        return (AuthIDGroup) createOrGetAuthorizable(authIDGroup, Group.class);
    }

    private <T extends Authorizable> AuthID createOrGetAuthorizable(AuthID authID, Class<T> authType) {
        try (ResourceResolver resourceResolver = getFreshAdminRR()) {
            return createOrGetAuthorizable(authID, authType, resourceResolver);
        }
    }

    @SneakyThrows
    @SuppressWarnings({"ReturnCount", "ChainOfInstanceofChecks", "PMD.CognitiveComplexity"})
    private <T extends Authorizable> AuthID createOrGetAuthorizable(
        AuthID authID, Class<T> authType, ResourceResolver resourceResolver
    ) {
        UserManager userManager = new WithUserManager(resourceResolver).get();
        return Optional.ofNullable(userManager.getAuthorizable(authID.get()))
            .flatMap(
                SneakyFunction.sneaky(
                    authorizable -> {
                        if (authorizable.isGroup() && authType == Group.class) {
                            return Optional.of(new AuthIDGroup(authID.get()));
                        } else if (!authorizable.isGroup() && authType == User.class) {
                            return Optional.of(new AuthIDUser(authID.get()));
                        } else {
                            return Optional.empty();
                        }
                    }
                )
            )
            .or(SneakySupplier.sneaky(() -> {
                if (authType == Group.class) {
                    Group group = userManager.createGroup(authID.get());
                    resourceResolver.commit();
                    String id = group.getID();
                    return Optional.of(new AuthIDGroup(id));
                } else if (authType == User.class) {
                    User user = userManager.createUser(authID.get(), DataSourceConfig.PASSWORD);
                    resourceResolver.commit();
                    String id = user.getID();
                    return Optional.of(new AuthIDUser(id));
                } else {
                    return Optional.empty();
                }
            }))
            .orElseThrow();
    }

    @SneakyThrows
    @SuppressWarnings({"squid:S1905", "unchecked", "rawtypes"})
    private void registerNodeTypes() {
        CNDSource cndSource = new CNDSource();
        try (
            InputStreamReader cndISR = cndSource.get();
            ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()
        ) {
            Session session = Optional.ofNullable(resourceResolver.adaptTo(Session.class)).orElseThrow();
            CndImporter.registerNodeTypes(cndISR, session);
            session.save();
        }
        try (ResourceResolver resourceResolver = fullResourceAccess.acquireAccess()) {
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
