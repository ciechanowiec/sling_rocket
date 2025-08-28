package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.commons.FileWithOriginalName;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import jakarta.ws.rs.core.MediaType;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import java.io.File;
import java.util.List;

/**
 * Wrapper around {@link SlingHttpServletRequest} that provides additional functionality to the wrapped object.
 */
interface WrappedSlingRequest extends WrappedRequest, SlingRequestWithDecomposition {

    /**
     * Returns {@link MediaType#MULTIPART_FORM_DATA} form fields of the wrapped {@link SlingHttpServletRequest} (i.e.,
     * files uploaded with the request) as {@link File}s temporarily written on the disk. An empty {@link List} is
     * returned if there are no such form fields for the wrapped {@link SlingHttpServletRequest}.
     *
     * @return {@link MediaType#MULTIPART_FORM_DATA} form fields of the wrapped {@link SlingHttpServletRequest} (i.e.,
     * files uploaded with the request) as {@link File}s temporarily written on the disk; an empty {@link List} is
     * returned if there are no such form fields for the wrapped {@link SlingHttpServletRequest}
     */
    List<FileWithOriginalName> uploadedFiles();

    /**
     * Returns the value returned by {@link SlingHttpServletRequest#getRemoteUser()} for the wrapped
     * {@link SlingHttpServletRequest}.
     *
     * @return value returned by {@link SlingHttpServletRequest#getRemoteUser()} for the wrapped
     * {@link SlingHttpServletRequest}
     */
    @Override
    String remoteUser();

    /**
     * Returns the {@link Resource} returned by the wrapped {@link SlingHttpServletRequest#getResource()}.
     *
     * @return {@link Resource} returned by the wrapped {@link SlingHttpServletRequest#getResource()}
     */
    Resource resource();

    /**
     * Returns {@link UserResourceAccess} for the {@link User} who issued the wrapped {@link SlingHttpServletRequest}.
     *
     * @return {@link UserResourceAccess} for the {@link User} who issued the wrapped {@link SlingHttpServletRequest}
     */
    UserResourceAccess userResourceAccess();
}
