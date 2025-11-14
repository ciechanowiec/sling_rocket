package eu.ciechanowiec.sling.rocket.network;

import java.util.List;

@FunctionalInterface
interface WithStackTrace {

    /**
     * Returns the {@link List} of {@link StackTraceElement}s representing the stack trace upon creation of this
     * object.
     * <p>
     * The {@link List} is empty if the stack trace was not provided upon object creation.
     *
     * @return {@link List} of {@link StackTraceElement}s representing the stack trace upon creation of this object; the
     * {@link List} is empty if the stack trace was not provided upon object creation
     */
    List<StackTraceElement> creationStackTrace();
}
