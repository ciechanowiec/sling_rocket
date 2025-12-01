package eu.ciechanowiec.sling.rocket.jcr.ref;

import javax.jcr.Node;

/**
 * Denotes an invalid state of program execution when some {@link Node} is not a valid {@link Referenceable}.
 */
@SuppressWarnings("WeakerAccess")
public class NotReferenceableException extends RuntimeException {

    /**
     * Constructs an instance of this class.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public NotReferenceableException(String message) {
        super(message);
    }
}
