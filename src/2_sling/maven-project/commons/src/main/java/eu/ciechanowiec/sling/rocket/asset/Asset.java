package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;

/**
 * <p>
 * Represents {@link Node} instances of {@link Asset#NT_ASSET_REAL} and {@link Asset#NT_ASSET_LINK} types.
 * That can be either a persisted or a hypothetically persisted {@link Node}.
 * </p>
 * <ol>
 *     <li>
 *        The exact type of the underlying {@link Node} is considered
 *        an implementation detail and is hidden from the client.
 *        The client might choose a {@link Node} of any supported type as the base for this {@link Asset}.
 *     </li>
 *     <li>
 *        A {@link Resource} representing a persisted {@link Node} of type {@link Asset#NT_ASSET_REAL}
 *        or {@link Asset#NT_ASSET_LINK} can be adapted to this {@link Asset}, e.g. this way:
 *        <pre>{@code
 *         Asset asset = resource.adaptTo(Asset.class);
 *        }</pre>
 *     </li>
 * </ol>
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public interface Asset extends WithJCRPath, Referencable {

    /**
     * The type name of a {@link Node} that contains a binary file and related metadata.
     */
    String NT_ASSET_REAL = "rocket:AssetReal";

    /**
     * The type name of a {@link Node} that links to a different {@link Node} of type
     * {@link Asset#NT_ASSET_REAL} or {@link Asset#NT_ASSET_LINK} (the latter one is a case of chained linking).
     */
    @SuppressWarnings("JavadocDeclaration")
    String NT_ASSET_LINK = "rocket:AssetLink";

    /**
     * The type name of a {@link Node} with metadata, which is a
     * child of a {@link Node} of type {@link Asset#NT_ASSET_REAL}.
     */
    String NT_ASSET_METADATA = "rocket:AssetMetadata";

    /**
     * Name of a {@link Property} of type {@link PropertyType#REFERENCE} on a {@link Node} of type
     * {@link Asset#NT_ASSET_LINK}, which points to a different {@link Node} of type {@link Asset#NT_ASSET_REAL}
     * or {@link Asset#NT_ASSET_LINK} (the latter one is a case of chained linking).
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
     * Returns the {@link AssetFile} associated with this {@link Asset}.
     * @return {@link AssetFile} associated with this {@link Asset}
     */
    AssetFile assetFile();

    /**
     * Returns the {@link AssetMetadata} associated with this {@link Asset}.
     * @return {@link AssetMetadata} associated with this {@link Asset}
     */
    AssetMetadata assetMetadata();
}
