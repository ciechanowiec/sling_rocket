package eu.ciechanowiec.sling.rocket.network;

import eu.ciechanowiec.sling.rocket.commons.JSON;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * Wrapper around {@link HttpServletRequest} that provides additional functionality to the wrapped object.
 */
interface WrappedRequest extends WithStackTrace, JSON {

    /**
     * Returns the value returned by {@link ServletRequest#getRemoteAddr()} for the wrapped {@link ServletRequest}.
     *
     * @return value returned by {@link ServletRequest#getRemoteAddr()} for the wrapped {@link ServletRequest}
     */
    String remoteAddress();

    /**
     * Returns the value returned by {@link ServletRequest#getRemoteHost()} for the wrapped {@link ServletRequest}.
     *
     * @return value returned by {@link ServletRequest#getRemoteHost()} for the wrapped {@link ServletRequest}.
     */
    String remoteHost();

    /**
     * Returns the value returned by {@link ServletRequest#getRemotePort()} for the wrapped {@link ServletRequest}.
     *
     * @return value returned by {@link ServletRequest#getRemotePort()} for the wrapped {@link ServletRequest}
     */
    int remotePort();

    /**
     * Returns the value returned by {@link HttpServletRequest#getRemoteUser()} for the wrapped
     * {@link HttpServletRequest}.
     *
     * @return value returned by {@link HttpServletRequest#getRemoteUser()} for the wrapped {@link HttpServletRequest}
     */
    String remoteUser();

    /**
     * Returns the value returned by {@link HttpServletRequest#getMethod()} for the wrapped {@link HttpServletRequest}.
     *
     * @return value returned by {@link HttpServletRequest#getMethod()} for the wrapped {@link HttpServletRequest}
     */
    String method();

    /**
     * Returns the value returned by {@link HttpServletRequest#getRequestURI()} for the wrapped
     * {@link HttpServletRequest}.
     *
     * @return value returned by {@link HttpServletRequest#getRequestURI()} for the wrapped
     * {@link HttpServletRequest}
     */
    HttpURI uri();

    /**
     * Returns the value returned by {@link ServletRequest#getContentLength()} for the wrapped
     * {@link ServletRequest}.
     *
     * @return value returned by {@link ServletRequest#getContentLength()} for the wrapped
     * {@link ServletRequest}
     */
    int contentLength();

    /**
     * Returns all {@link HttpFields} of the wrapped {@link HttpServletRequest}.
     *
     * @return all {@link HttpFields} of the wrapped {@link HttpServletRequest}
     */
    HttpFields httpFields();

    /**
     * Returns the {@link Class} of the wrapped {@link HttpServletRequest}.
     *
     * @return {@link Class} of the wrapped {@link HttpServletRequest}
     */
    Class<?> wrappedRequestClass();
}
