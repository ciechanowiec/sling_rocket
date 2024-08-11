package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.jcr.Referencable;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Node;
import java.util.Collection;

/**
 * <p>
 * Represents {@link Node} instances of type {@link Assets#NT_ASSETS}.
 * </p>
 * A {@link Resource} representing a {@link Node} of type {@link Assets#NT_ASSETS}
 * can be adapted to this {@link Assets}, e.g. this way:
 * <pre>{@code
 *  Assets assets = resource.adaptTo(Assets.class);
 * }</pre>
 */
public interface Assets extends WithJCRPath, Referencable {

    /**
     * The type name of a {@link Node} that holds as direct children other {@link Node}-s
     * of {@link Asset#NT_ASSET_REAL} and {@link Asset#NT_ASSET_LINK} types.
     */
    String NT_ASSETS = "rocket:Assets";

    /**
     * Returns a {@link Collection} of {@link Asset}-s held in this {@link Assets} instance.
     * @return a {@link Collection} of {@link Asset}-s held in this {@link Assets} instance
     */
    Collection<Asset> get();
}
