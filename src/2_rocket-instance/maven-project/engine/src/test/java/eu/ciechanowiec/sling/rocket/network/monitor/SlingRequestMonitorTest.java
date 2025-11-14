package eu.ciechanowiec.sling.rocket.network.monitor;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class SlingRequestMonitorTest extends TestEnvironment {

    SlingRequestMonitorTest() {
        super(ResourceResolverType.RESOURCEPROVIDER_MOCK);
    }

    @BeforeEach
    void setup() {
        context.build().resource("/content/rocket").commit();
        context.currentResource("/content/rocket");
    }

    @SneakyThrows
    @Test
    void mustRegister() {
        SlingRequestMonitor monitor = context.registerInjectActivateService(
            SlingRequestMonitor.class, Map.of("is-enabled", true)
        );
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        request.setHeader("FIRST-HEADER", "first-header-value");
        request.setHeader("SECOND-HEADER", "second-header-value");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        monitor.doFilter(request, response, chain);
        assertAll(
            () -> verify(request, times(1)).getHeaderNames(),
            () -> verify(request, times(1)).getContentLength()
        );
    }

    @SneakyThrows
    @Test
    void mustNotRegisterWhenDisabled() {
        SlingRequestMonitor monitor = context.registerInjectActivateService(
            SlingRequestMonitor.class, Map.of("is-enabled", false)
        );
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        request.setHeader("FIRST-HEADER", "first-header-value");
        request.setHeader("SECOND-HEADER", "second-header-value");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        monitor.doFilter(request, response, chain);
        assertAll(
            () -> verify(request, never()).getHeaderNames(),
            () -> verify(request, never()).getContentLength()
        );
    }

    @SneakyThrows
    @Test
    void mustNotRegisterWhenWrongClass() {
        SlingRequestMonitor monitor = context.registerInjectActivateService(
            SlingRequestMonitor.class, Map.of("is-enabled", true)
        );
        ServletRequest request = mock(ServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        monitor.doFilter(request, response, chain);
        verify(request, never()).getContentLength();
    }
}
