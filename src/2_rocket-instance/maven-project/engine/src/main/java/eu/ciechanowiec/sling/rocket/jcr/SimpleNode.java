package eu.ciechanowiec.sling.rocket.jcr;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.util.Map;

/**
 * <p>
 * Wrapper over a {@link Node}, either existing or hypothetical.
 * </p>
 * <p>
 * The class provides operations on a {@link Node} in a way detached from an ongoing {@link Session}. {@link Session}'s
 * life cycle is supposed to be fully managed by {@link SimpleNode} itself in an encapsulated manner.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
public class SimpleNode {

    private final JCRPath pathToNode;
    private final ResourceAccess resourceAccess;
    private final String defaultNodeType;

    /**
     * Constructs an instance of this class.
     *
     * @param pathToNode     path to the {@link Node} this {@link SimpleNode} is supposed to represent
     * @param resourceAccess {@link ResourceAccess} that will be used to acquire access to resources
     */
    public SimpleNode(JCRPath pathToNode, ResourceAccess resourceAccess) {
        this(pathToNode, resourceAccess, JcrConstants.NT_UNSTRUCTURED);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param pathToNode      path to the {@link Node} this {@link SimpleNode} is supposed to represent
     * @param resourceAccess  {@link ResourceAccess} that will be used to acquire access to resources
     * @param defaultNodeType default {@link NodeType} to be used if the {@link Node} represented by this
     *                        {@link SimpleNode} does not exist yet
     */
    public SimpleNode(JCRPath pathToNode, ResourceAccess resourceAccess, String defaultNodeType) {
        this.pathToNode = pathToNode;
        this.resourceAccess = resourceAccess;
        this.defaultNodeType = defaultNodeType;
    }

    /**
     * Constructs an instance of this class.
     *
     * @param withJCRPath     object that contains a {@link JCRPath} to the {@link Node} this {@link SimpleNode} is
     *                        supposed to represent
     * @param resourceAccess  {@link ResourceAccess} that will be used to acquire access to resources
     * @param defaultNodeType default {@link NodeType} to be used if the {@link Node} represented by this
     *                        {@link SimpleNode} does not exist yet
     */
    public SimpleNode(WithJCRPath withJCRPath, ResourceAccess resourceAccess, String defaultNodeType) {
        this.pathToNode = withJCRPath.jcrPath();
        this.resourceAccess = resourceAccess;
        this.defaultNodeType = defaultNodeType;
    }

    /**
     * Returns the {@link NodeProperties} of the {@link Node} represented by this {@link SimpleNode}. If the
     * {@link Node} does not exist yet, it will be created.
     *
     * @return {@link NodeProperties} of the {@link Node} represented by this {@link SimpleNode}
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_INFERRED")
    public NodeProperties nodeProperties() {
        ensureNodeExists();
        return new NodeProperties(pathToNode, resourceAccess);
    }

    /**
     * Ensures that the {@link Node} represented by this {@link SimpleNode} exists. If the {@link Node} does not exist
     * yet, it will be created.
     *
     * @return {@link SimpleNode} representing an existing {@link Node}
     */
    @SneakyThrows
    public SimpleNode ensureNodeExists() {
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String pathToEnsureRaw = pathToNode.get();
            Resource resource = ResourceUtil.getOrCreateResource(
                resourceResolver, pathToEnsureRaw,
                Map.of(JcrConstants.JCR_PRIMARYTYPE, defaultNodeType), null, true
            );
            log.trace("Ensured {}", resource);
        }
        return new SimpleNode(pathToNode, resourceAccess, defaultNodeType);
    }
}
