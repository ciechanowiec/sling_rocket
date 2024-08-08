package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Node;
import java.io.File;
import java.util.Optional;

/**
 * <p>
 * Represents {@link Node} instances of type {@link JcrConstants#NT_FILE}.
 * </p>
 * A {@link Resource} representing a {@link Node} of type {@link JcrConstants#NT_FILE}
 * can be adapted to this {@link NTFile}, e.g. this way:
 * <pre>{@code
 *  NTFile ntFile = resource.adaptTo(NTFile.class);
 * }</pre>
 */
public interface NTFile extends WithJCRPath {

    /**
     * Returns an {@link Optional} containing the binary file stored in the underlying {@link Node}.
     * @return {@link Optional} containing the binary file stored in the underlying {@link Node};
     *         empty {@link Optional} is returned if the file cannot be retrieved
     */
    Optional<File> retrieve();
}
