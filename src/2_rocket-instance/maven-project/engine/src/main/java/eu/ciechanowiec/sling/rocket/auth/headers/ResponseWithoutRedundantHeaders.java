package eu.ciechanowiec.sling.rocket.auth.headers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.auth.core.AuthConstants;

@Slf4j
class ResponseWithoutRedundantHeaders extends HttpServletResponseWrapper {

    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    ResponseWithoutRedundantHeaders(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void addHeader(String name, String value) {
        if (isAllowedHeader(name)) {
            super.addHeader(name, value);
        } else {
            log.trace("Filtered out header '{}' with value '{}'", name, value);
        }
    }

    @Override
    public void setHeader(String name, String value) {
        if (isAllowedHeader(name)) {
            super.setHeader(name, value);
        } else {
            log.trace("Filtered out header '{}' with value '{}'", name, value);
        }
    }

    private boolean isAllowedHeader(String name) {
        return !name.equalsIgnoreCase(WWW_AUTHENTICATE_HEADER)
            && !name.equalsIgnoreCase(AuthConstants.X_REASON)
            && !name.equalsIgnoreCase(AuthConstants.X_REASON_CODE);
    }
}
