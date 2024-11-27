package eu.ciechanowiec.sling.rocket.network;

import jakarta.ws.rs.core.MediaType;
import org.apache.sling.api.SlingHttpServletRequest;

import java.io.File;
import java.util.List;

/**
 * Wrapper around {@link SlingHttpServletRequest} that provides additional functionality to the wrapped object related
 * to {@link MediaType#MULTIPART_FORM_DATA} form fields (i.e. related to the files uploaded with the request.)
 */
@FunctionalInterface
public interface RequestWithFiles {

    /**
     * Returns {@link MediaType#MULTIPART_FORM_DATA} form fields of the wrapped {@link SlingHttpServletRequest}
     * (i.e. files uploaded with the request) as {@link File}s temporarily written on the disk. An empty {@link List}
     * is returned if there are no such form fields for the wrapped {@link SlingHttpServletRequest}.
     * @return {@link MediaType#MULTIPART_FORM_DATA} form fields of the wrapped {@link SlingHttpServletRequest}
     *         (i.e. files uploaded with the request) as {@link File}s temporarily written on the disk; an empty
     *         {@link List} is returned if there are no such form fields for the wrapped {@link SlingHttpServletRequest}
     */
    List<File> uploadedFiles();
}
