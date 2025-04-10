package eu.ciechanowiec.sling.rocket.jcr;

import javax.jcr.Node;
import javax.jcr.Property;
import org.apache.jackrabbit.JcrConstants;

/**
 * Represents a {@link Node} that has an associated {@link Property} named {@link JcrConstants#JCR_UUID} of type
 * {@link String}. The associated {@link Property} can belong to the {@link Node} represented by this
 * {@link Referencable} itself or to one of its descendants.
 */
@FunctionalInterface
public interface Referencable {

    /**
     * Returns the value of the associated {@link Property} named {@link JcrConstants#JCR_UUID} of type {@link String}.
     *
     * @return value of the associated {@link Property} named {@link JcrConstants#JCR_UUID} of type {@link String}
     * @throws NotReferencableException if the underlying {@link Node} doesn't have the associated {@link Property}
     *                                  named {@link JcrConstants#JCR_UUID}
     */
    String jcrUUID();
}
