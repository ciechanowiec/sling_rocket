package eu.ciechanowiec.sling.rocket.jcr.path;

import javax.jcr.Item;

/**
 * Denotes an invalid state of program execution when some {@link JCRPath} is occupied by some {@link Item}, while it is
 * expected to be free.
 */
@SuppressWarnings("WeakerAccess")
public class OccupiedJCRPathException extends RuntimeException {

    /**
     * Constructs an instance of this class.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public OccupiedJCRPathException(String message) {
        super(message);
    }
}
