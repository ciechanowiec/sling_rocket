package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.commons.FileWithOriginalName;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import jakarta.ws.rs.core.MediaType;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import java.io.File;
import java.util.List;

/**
 * Wrapper around {@link SlingJakartaHttpServletRequest} that provides additional functionality to the wrapped object.
 */
interface WrappedSlingRequest extends WrappedRequest, SlingRequestWithDecomposition {

    /**
     * Returns {@link MediaType#MULTIPART_FORM_DATA} form fields of the wrapped {@link SlingJakartaHttpServletRequest}
     * (i.e., files uploaded with the request) as {@link File}s temporarily written on the disk. An empty {@link List}
     * is returned if there are no such form fields for the wrapped {@link SlingJakartaHttpServletRequest}.
     *
     * @return {@link MediaType#MULTIPART_FORM_DATA} form fields of the wrapped {@link SlingJakartaHttpServletRequest}
     * (i.e., files uploaded with the request) as {@link File}s temporarily written on the disk; an empty {@link List}
     * is returned if there are no such form fields for the wrapped {@link SlingJakartaHttpServletRequest}
     */
    List<FileWithOriginalName> uploadedFiles();

    /**
     * Returns the value returned by {@link SlingJakartaHttpServletRequest#getRemoteUser()} for the wrapped
     * {@link SlingJakartaHttpServletRequest}.
     *
     * @return value returned by {@link SlingJakartaHttpServletRequest#getRemoteUser()} for the wrapped
     * {@link SlingJakartaHttpServletRequest}
     */
    @Override
    String remoteUser();

    /**
     * Returns the {@link Resource} returned by the wrapped {@link SlingJakartaHttpServletRequest#getResource()}.
     *
     * @return {@link Resource} returned by the wrapped {@link SlingJakartaHttpServletRequest#getResource()}
     */
    Resource resource();

    /**
     * Returns {@link UserResourceAccess} for the {@link User} who issued the wrapped
     * {@link SlingJakartaHttpServletRequest}.
     *
     * @return {@link UserResourceAccess} for the {@link User} who issued the wrapped
     * {@link SlingJakartaHttpServletRequest}
     */
    UserResourceAccess userResourceAccess();
}
