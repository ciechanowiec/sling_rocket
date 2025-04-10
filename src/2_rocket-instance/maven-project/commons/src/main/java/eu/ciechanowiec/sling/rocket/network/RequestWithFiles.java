package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.commons.FileWithOriginalName;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.util.List;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Wrapper around {@link SlingHttpServletRequest} that provides additional functionality to the wrapped object related
 * to {@link MediaType#MULTIPART_FORM_DATA} form fields (i.e. related to the files uploaded with the request.)
 */
@FunctionalInterface
public interface RequestWithFiles {

    /**
     * Returns {@link MediaType#MULTIPART_FORM_DATA} form fields of the wrapped {@link SlingHttpServletRequest} (i.e.
     * files uploaded with the request) as {@link File}s temporarily written on the disk. An empty {@link List} is
     * returned if there are no such form fields for the wrapped {@link SlingHttpServletRequest}.
     *
     * @return {@link MediaType#MULTIPART_FORM_DATA} form fields of the wrapped {@link SlingHttpServletRequest} (i.e.
     * files uploaded with the request) as {@link File}s temporarily written on the disk; an empty {@link List} is
     * returned if there are no such form fields for the wrapped {@link SlingHttpServletRequest}
     */
    List<FileWithOriginalName> uploadedFiles();
}
