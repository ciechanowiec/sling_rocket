package eu.ciechanowiec.sling.rocket.jcr.path;

import javax.jcr.Item;

/**
 * Denotes an invalid state of program execution when some {@link JCRPath} is occupied by
 * some {@link Item}, while it is expected to be free.
 */
public class OccupiedJCRPathException extends RuntimeException {

    OccupiedJCRPathException(String message) {
        super(message);
    }
}
