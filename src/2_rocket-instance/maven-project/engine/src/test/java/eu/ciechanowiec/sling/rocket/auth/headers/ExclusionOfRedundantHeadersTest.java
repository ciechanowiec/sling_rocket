package eu.ciechanowiec.sling.rocket.auth.headers;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ExclusionOfRedundantHeadersTest extends TestEnvironment {

    ExclusionOfRedundantHeadersTest() {
        super(ResourceResolverType.RESOURCEPROVIDER_MOCK);
    }

    @SneakyThrows
    @Test
    void shouldWrapHttpServletResponse() {
        // Given
        Filter filter = new ExclusionOfRedundantHeaders();
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(chain, times(1)).doFilter(eq(request), any(ResponseWithoutRedundantHeaders.class));
        verify(chain, never()).doFilter(eq(request), eq(response));
    }

    @SneakyThrows
    @Test
    void shouldNotWrapNonHttpServletResponse() {
        // Given
        Filter filter = new ExclusionOfRedundantHeaders();
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(chain, times(1)).doFilter(request, response);
        verify(chain, never()).doFilter(eq(request), any(ResponseWithoutRedundantHeaders.class));
    }

    @SneakyThrows
    @Test
    void shouldInitializeAndDestroy() {
        ExclusionOfRedundantHeaders filter = new ExclusionOfRedundantHeaders();
        assertAll(
            () -> assertDoesNotThrow(() -> filter.init(null)),
            () -> assertDoesNotThrow(filter::destroy)
        );
    }

    @SuppressWarnings({"ReturnOfNull", "Regexp"})
    @SneakyThrows
    @Test
    void shouldFilterOutRedundantHeaders() {
        // Given
        Filter filter = new ExclusionOfRedundantHeaders();
        MockSlingJakartaHttpServletRequest request = context.jakartaRequest();
        HttpServletResponse originalResponse = spy(context.jakartaResponse());
        FilterChain chain = mock(FilterChain.class);

        // Custom chain implementation to verify the wrapped response behavior
        doAnswer(invocation -> {
            ServletResponse wrappedResponse = invocation.getArgument(1);

            // Verify that the response is wrapped
            assertAll(
                () -> verify(originalResponse, never()).addHeader(eq("WWW-Authenticate"), anyString()),
                () -> verify(originalResponse, never()).addHeader(eq(AuthConstants.X_REASON), anyString()),
                () -> verify(originalResponse, never()).addHeader(eq(AuthConstants.X_REASON_CODE), anyString()),
                () -> verify(originalResponse, never()).setHeader(eq("WWW-Authenticate"), anyString()),
                () -> verify(originalResponse, never()).setHeader(eq(AuthConstants.X_REASON), anyString()),
                () -> verify(originalResponse, never()).setHeader(eq(AuthConstants.X_REASON_CODE), anyString())
            );

            // Test that the wrapped response filters out redundant headers
            wrappedResponse.setCharacterEncoding("UTF-8");

            // Add headers to the wrapped response
            if (wrappedResponse instanceof HttpServletResponse httpResponse) {
                httpResponse.addHeader("WWW-Authenticate", "Basic");
                httpResponse.addHeader(AuthConstants.X_REASON, "reason");
                httpResponse.addHeader(AuthConstants.X_REASON_CODE, "code");
                httpResponse.addHeader("Content-Type", "text/html");

                httpResponse.setHeader("WWW-Authenticate", "Basic");
                httpResponse.setHeader(AuthConstants.X_REASON, "reason");
                httpResponse.setHeader(AuthConstants.X_REASON_CODE, "code");
                httpResponse.setHeader("Content-Type", "text/html");
            }

            return null;
        }).when(chain).doFilter(any(), any());

        // When
        filter.doFilter(request, originalResponse, chain);

        // Then
        verify(originalResponse, times(1)).setHeader(eq("Content-Type"), eq("text/html"));
        verify(originalResponse, times(1)).addHeader(eq("Content-Type"), eq("text/html"));
    }
}
