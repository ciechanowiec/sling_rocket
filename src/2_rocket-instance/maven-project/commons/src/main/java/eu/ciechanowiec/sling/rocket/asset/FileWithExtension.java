package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.conditional.Conditional;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import java.io.File;
import java.util.Optional;

/**
 * Represents a {@link File} with an extension in its name.
 */
@Slf4j
public class FileWithExtension {

    private final File file;

    /**
     * Constructs an instance of this class.
     * @param file the {@link File} to be wrapped by the constructed instance
     */
    public FileWithExtension(File file) {
        this.file = file;
    }

    /**
     * Renames the wrapped {@link File} to have the file name extension specific for the
     * specified MIME type. The new name will be composed of the specified basic name and the extension
     * separated by a dot ([new-basic-name].[extension]). For instance, for the new basic name "my-photo"
     * and the MIME type "image/jpeg", the resulting file will have the name "my-photo.jpg".
     * @param newBasicName the new basic name for the {@link File}
     * @param mimeTypeName the name of the MIME type
     * @return the renamed {@link File}
     */
    File rename(String newBasicName, String mimeTypeName) {
        log.trace("Renaming '{}' to have the extension for MIME type '{}'", file, mimeTypeName);
        MimeTypes defaultMimeTypes = MimeTypes.getDefaultMimeTypes();
        try {
            MimeType mimeType = defaultMimeTypes.forName(mimeTypeName);
            String extension = mimeType.getExtension();
            return Conditional.conditional(extension.isBlank())
                    .onTrue(() -> {
                        log.warn("Unable to rename '{}' to have the extension for MIME type '{}'", file, mimeTypeName);
                        return file;
                    })
                    .onFalse(() -> renameForResolvedExtension(newBasicName, extension))
                    .get(File.class);
        } catch (MimeTypeException exception) {
            String message = String.format(
                    "Unable to rename '%s' to have the extension for MIME type '%s'",
                    file, mimeTypeName
            );
            log.warn(message, exception);
            return file;
        }
    }

    private File renameForResolvedExtension(String newBasicName, String extension) {
        String newFileName = String.format("%s%s", newBasicName, extension);
        File newFile = Optional.ofNullable(file.getParent())
                               .map(parent -> new File(parent, newFileName))
                               .orElse(new File(newFileName));
        log.trace("New name for '{}' is '{}'", file, newFileName);
        newFile.deleteOnExit();
        boolean wasRenamed = file.renameTo(newFile);
        log.trace("Was '{}' renamed to '{}'? Answer: {}", file, newFile, wasRenamed);
        return Conditional.conditional(wasRenamed)
                .onTrue(() -> newFile)
                .onFalse(() -> file)
                .get(File.class);
    }
}
