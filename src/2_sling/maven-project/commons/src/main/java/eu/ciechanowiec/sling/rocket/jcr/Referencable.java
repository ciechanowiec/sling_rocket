package eu.ciechanowiec.sling.rocket.jcr;

import org.apache.jackrabbit.JcrConstants;

import javax.jcr.Node;
import javax.jcr.Property;

/**
 * Represents a {@link Node} of that has a {@link Property}
 * named {@link JcrConstants#JCR_UUID} of type {@link String}.
 */
@FunctionalInterface
public interface Referencable {

    /**
     * Returns the value of the {@link Property} named {@link JcrConstants#JCR_UUID} of type {@link String}.
     * @return value of the {@link Property} named {@link JcrConstants#JCR_UUID} of type {@link String}
     * @throws NotReferencableException if the underlying {@link Node} doesn't contain the {@link Property}
     *         named {@link JcrConstants#JCR_UUID}
     */
    String jcrUUID();
}
