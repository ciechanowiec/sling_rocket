package eu.ciechanowiec.sling.rocket.commons;

import java.io.File;

/**
 * A {@link File} with its original name. The original name is the name that was given to the {@link File} in the
 * source system from which this {@link File} was obtained, e.g. from the user's file system.
 * @param file {@link File} that is represented by this object
 * @param originalName original name of the {@link File}
 */
public record FileWithOriginalName(File file, String originalName) {
}
