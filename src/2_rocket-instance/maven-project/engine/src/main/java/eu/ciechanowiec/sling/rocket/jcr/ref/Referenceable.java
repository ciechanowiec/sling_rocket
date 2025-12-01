package eu.ciechanowiec.sling.rocket.jcr.ref;

import org.apache.jackrabbit.JcrConstants;

import javax.jcr.Node;
import javax.jcr.Property;

/**
 * Represents a {@link Node} that has an associated {@link Property} named {@link JcrConstants#JCR_UUID} of type
 * {@link String}. The associated {@link Property} can belong to the {@link Node} represented by this
 * {@link Referenceable} itself or to one of its descendants.
 */
@FunctionalInterface
public interface Referenceable {

    /**
     * Returns the value of the associated {@link Property} named {@link JcrConstants#JCR_UUID} of type {@link String}.
     *
     * @return value of the associated {@link Property} named {@link JcrConstants#JCR_UUID} of type {@link String}
     * @throws NotReferenceableException if the underlying {@link Node} doesn't have the associated {@link Property}
     *                                  named {@link JcrConstants#JCR_UUID}
     */
    String jcrUUID();
}
