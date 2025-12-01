package eu.ciechanowiec.sling.rocket.jcr.ref;

import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceableResolvableTest extends TestEnvironment {

    ReferenceableResolvableTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void mustResolveNodeWithResourceResolver() throws RepositoryException, PersistenceException {
        Resource resourceOriginal = context.build().resource(
            "/content/node_rr",
            Map.of(
                JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED,
                JcrConstants.JCR_MIXINTYPES, new String[]{JcrConstants.MIX_REFERENCEABLE}
            )
        ).commit().getCurrentParent();
        Node nodeOriginal = Optional.ofNullable(resourceOriginal.adaptTo(Node.class)).orElseThrow();
        String uuid = nodeOriginal.getIdentifier();
        ReferenceableResolvable resolvable = new ReferenceableResolvable(() -> uuid, context.resourceResolver());
        Optional<Node> nodeResolved = resolvable.node();
        Optional<Resource> resourceResolved = resolvable.resource();
        assertAll(
            () -> assertTrue(nodeResolved.isPresent()),
            () -> assertTrue(resourceResolved.isPresent()),
            () -> assertEquals(nodeOriginal.getPath(), nodeResolved.orElseThrow().getPath()),
            () -> assertEquals(resourceOriginal.getPath(), resourceResolved.orElseThrow().getPath()),
            () -> assertEquals(uuid, nodeResolved.orElseThrow().getIdentifier()),
            () -> assertEquals(
                uuid, resourceResolved.orElseThrow().getValueMap().get(JcrConstants.JCR_UUID, String.class)
            )
        );
    }

    @SuppressWarnings("resource")
    @Test
    @SneakyThrows
    void mustResolveNodeWithSession() {
        Bundle mockBundle = context.bundleContext().getBundle();
        Class<ReferenceableResolvable> targetClass = ReferenceableResolvable.class;
        try (MockedStatic<FrameworkUtil> utilities = Mockito.mockStatic(FrameworkUtil.class)) {
            utilities.when(() -> FrameworkUtil.getBundle(targetClass))
                .thenReturn(mockBundle);
            Resource resourceOriginal = context.build().resource(
                "/content/node_session",
                Map.of(
                    JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED,
                    JcrConstants.JCR_MIXINTYPES, new String[]{JcrConstants.MIX_REFERENCEABLE}
                )
            ).commit().getCurrentParent();
            Node nodeOriginal = Optional.ofNullable(resourceOriginal.adaptTo(Node.class)).orElseThrow();
            String uuid = nodeOriginal.getIdentifier();
            Session session = Optional.ofNullable(context.resourceResolver().adaptTo(Session.class)).orElseThrow();
            ReferenceableResolvable resolvable = new ReferenceableResolvable(() -> uuid, session);
            Optional<Node> nodeResolved = resolvable.node();
            Optional<Resource> resourceResolved = resolvable.resource();
            assertAll(
                () -> assertTrue(nodeResolved.isPresent()),
                () -> assertTrue(resourceResolved.isPresent()),
                () -> assertEquals(nodeOriginal.getPath(), nodeResolved.orElseThrow().getPath()),
                () -> assertEquals(resourceOriginal.getPath(), resourceResolved.orElseThrow().getPath()),
                () -> assertEquals(uuid, nodeResolved.orElseThrow().getIdentifier()),
                () -> assertEquals(
                    uuid, resourceResolved.orElseThrow().getValueMap().get(JcrConstants.JCR_UUID, String.class)
                )
            );
        }
    }

    @SuppressWarnings({"resource", "PMD.CloseResource"})
    @Test
    void mustReturnEmptyOptionalForNonexistentNode() throws RepositoryException, PersistenceException {
        Resource resource = context.build().resource(
            "/content/deleted_node",
            Map.of(
                JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED,
                JcrConstants.JCR_MIXINTYPES, new String[]{JcrConstants.MIX_REFERENCEABLE}
            )
        ).commit().getCurrentParent();
        Node node = Optional.ofNullable(resource.adaptTo(Node.class)).orElseThrow();
        String uuid = node.getIdentifier();
        ResourceResolver resourceResolver = context.resourceResolver();
        resourceResolver.delete(resource);
        resourceResolver.commit();
        ReferenceableResolvable resolvable = new ReferenceableResolvable(() -> uuid, context.resourceResolver());
        Optional<Node> nodeResult = resolvable.node();
        Optional<Resource> resourceResult = resolvable.resource();
        assertAll(
            () -> assertFalse(nodeResult.isPresent()),
            () -> assertFalse(resourceResult.isPresent())
        );
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void mustPropagateExceptionFromUnderlyingReferencable() throws PersistenceException {
        Resource resource = context.build().resource(
            "/content/non_ref_node", Map.of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED)
        ).commit().getCurrentParent();
        ResourceResolver resourceResolver = context.resourceResolver();
        resourceResolver.commit();

        ReferenceableSimple nonReferencable = new ReferenceableSimple(
            () -> new TargetJCRPath(resource), fullResourceAccess
        );
        ReferenceableResolvable resolvable = new ReferenceableResolvable(nonReferencable, resourceResolver);
        assertThrows(NotReferenceableException.class, resolvable::jcrUUID);
    }
}
