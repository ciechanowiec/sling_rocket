package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.ref.Referenceable;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import org.apache.jackrabbit.JcrConstants;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.nodetype.NodeType;
import java.util.List;

/**
 * <p>
 * Represents {@link Node} instances of primary {@link NodeType}s specified in {@link Asset#SUPPORTED_PRIMARY_TYPES}.
 * Only {@link Node}s actually persisted and existing in the {@link Repository} can be represented by this
 * {@link Asset}.
 * </p>
 * <ol>
 *     <li>
 *        The exact type of the underlying {@link Node} is considered
 *        an implementation detail and is hidden from the client.
 *        The client might choose a {@link Node} of any supported type as the base for this {@link Asset}.
 *     </li>
 *     <li>
 *        The basic implementation of this interface is {@link UniversalAsset}.
 *     </li>
 * </ol>
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public interface Asset extends WithJCRPath, Referenceable {

    /**
     * The type name of a {@link Node} that contains a binary file and related metadata.
     */
    String NT_ASSET_REAL = "rocket:AssetReal";

    /**
     * The type name of a {@link Node} that links to a different {@link Node} that can be represented by an instance of
     * an {@link Asset}.
     */
    String NT_ASSET_LINK = "rocket:AssetLink";

    /**
     * The type name of a {@link Node} with metadata, which is a child of a {@link Node} of type
     * {@link Asset#NT_ASSET_REAL}.
     */
    String NT_ASSET_METADATA = "rocket:AssetMetadata";

    /**
     * Name of a {@link Property} of type {@link PropertyType#REFERENCE} on a {@link Node} of type
     * {@link Asset#NT_ASSET_LINK}, which points to a different {@link Node} that can be represented by an instance of
     * an {@link Asset}.
     */
    String PN_LINKED_ASSET = "linkedAsset";

    /**
     * Name of a {@link Node} with a file, which is a child of a {@link Node} of type {@link Asset#NT_ASSET_REAL}.
     */
    String FILE_NODE_NAME = "file";

    /**
     * Name of a {@link Node} with metadata, which is a child of a {@link Node} of type {@link Asset#NT_ASSET_REAL}.
     */
    String METADATA_NODE_NAME = "metadata";

    /**
     * List of {@link Node} primary types supported by {@link Asset}.
     */
    List<String> SUPPORTED_PRIMARY_TYPES = List.of(
        NT_ASSET_REAL, NT_ASSET_LINK, JcrConstants.NT_RESOURCE, JcrConstants.NT_FILE
    );

    /**
     * Returns the {@link AssetFile} associated with this {@link Asset}.
     *
     * @return {@link AssetFile} associated with this {@link Asset}
     */
    AssetFile assetFile();

    /**
     * Returns the {@link AssetMetadata} associated with this {@link Asset}.
     *
     * @return {@link AssetMetadata} associated with this {@link Asset}
     */
    AssetMetadata assetMetadata();
}
