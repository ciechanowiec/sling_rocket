package eu.ciechanowiec.sling.rocket.jcr;

import javax.jcr.Node;

/**
 * Denotes an invalid state of program execution when some {@link Node} is of an unexpected primary type.
 */
public class IllegalPrimaryTypeException extends RuntimeException {

    /**
     * Constructs an instance of this class.
     *
     * @param expectedPrimaryType expected primary type of a {@link Node}
     */
    public IllegalPrimaryTypeException(String expectedPrimaryType) {
        super(String.format("Must be of primary type '%s'", expectedPrimaryType));
    }
}
