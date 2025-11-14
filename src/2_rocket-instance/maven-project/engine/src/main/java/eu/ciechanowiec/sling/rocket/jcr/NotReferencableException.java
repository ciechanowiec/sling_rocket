package eu.ciechanowiec.sling.rocket.jcr;

import javax.jcr.Node;

/**
 * Denotes an invalid state of program execution when some {@link Node} is not a valid {@link Referencable}.
 */
public class NotReferencableException extends RuntimeException {

    /**
     * Constructs an instance of this class.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public NotReferencableException(String message) {
        super(message);
    }
}
