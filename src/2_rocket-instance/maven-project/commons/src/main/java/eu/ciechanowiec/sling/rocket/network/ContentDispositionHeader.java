package eu.ciechanowiec.sling.rocket.network;

import jakarta.ws.rs.core.HttpHeaders;

import java.io.File;
import java.util.function.Function;

/**
 * Represents a {@link HttpHeaders#CONTENT_DISPOSITION} HTTP header
 */
public enum ContentDispositionHeader {

    /**
     * {@link HttpHeaders#CONTENT_DISPOSITION} HTTP header of type {@code attachment}.
     */
    ATTACHMENT(
        file -> {
            String fileName = file.getName();
            return "attachment;filename=\"%s\"".formatted(fileName);
        }
    ),

    /**
     * {@link HttpHeaders#CONTENT_DISPOSITION} HTTP header of type {@code inline}.
     */
    INLINE(file -> "inline");

    private final Function<File, String> value;

    ContentDispositionHeader(Function<File, String> value) {
        this.value = value;
    }

    /**
     * Returns the value of this {@link ContentDispositionHeader} for the specified {@link File}.
     *
     * @param file {@link File} for which the value of this {@link ContentDispositionHeader} will be returned
     * @return value of this {@link ContentDispositionHeader} for the specified {@link File}
     */
    public String value(File file) {
        return value.apply(file);
    }
}
