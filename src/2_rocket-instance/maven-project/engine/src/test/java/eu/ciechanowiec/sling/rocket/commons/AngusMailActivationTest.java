package eu.ciechanowiec.sling.rocket.commons;

import jakarta.activation.CommandMap;
import jakarta.activation.DataContentHandler;
import jakarta.activation.MailcapCommandMap;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.AvoidAccessibilityAlteration", "PMD.AvoidLiteralsInIfCondition"})
class AngusMailActivationTest {

    @Test
    void testActivation() {
        assertDoesNotThrow(AngusMailActivation::new);
        assertInstanceOf(MailcapCommandMap.class, CommandMap.getDefaultCommandMap());
    }

    @Test
    void testActivationTwice() {
        assertDoesNotThrow(
            () -> {
                new AngusMailActivation();
                new AngusMailActivation();
            }
        );
    }

    @SneakyThrows
    @ParameterizedTest
    @CsvSource(
        {
            "text/plain, org.eclipse.angus.mail.handlers.text_plain",
            "text/plain; charset=utf-8, org.eclipse.angus.mail.handlers.text_plain",
            "text/html, org.eclipse.angus.mail.handlers.text_html",
            "text/xml, org.eclipse.angus.mail.handlers.text_xml",
            "multipart/mixed, org.eclipse.angus.mail.handlers.multipart_mixed",
            "multipart/alternative, org.eclipse.angus.mail.handlers.multipart_mixed",
            "message/rfc822, org.eclipse.angus.mail.handlers.message_rfc822",
            "application/pdf, 'EMPTY'"
        }
    )
    void testCreateHandler(String mimeType, String expectedClassName) {
        AngusMailActivation activation = new AngusMailActivation();
        Method method = AngusMailActivation.class.getDeclaredMethod("createHandler", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Optional<DataContentHandler> result = (Optional<DataContentHandler>) method.invoke(activation, mimeType);

        if (expectedClassName.equals("EMPTY")) {
            assertFalse(result.isPresent());
        } else {
            assertTrue(result.isPresent());
            assertEquals(expectedClassName, result.get().getClass().getName());
        }
    }

    @SneakyThrows
    @ParameterizedTest
    @CsvSource(
        {
            "text/plain, text/plain, true",
            "text/plain, text/plain; charset=utf-8, true",
            "text/plain, text/html, false",
            "multipart/*, multipart/mixed, true",
            "multipart/*, multipart/related, true",
            "multipart/*, text/plain, false",
            "message/rfc822, message/rfc822, true",
            "message/rfc822, message/rfc822; parameter=value, true"
        }
    )
    void testMatchesMimeType(String pattern, String mimeType, boolean expected) {
        AngusMailActivation activation = new AngusMailActivation();
        Method method = AngusMailActivation.class.getDeclaredMethod("matchesMimeType", String.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(activation, pattern, mimeType);

        assertEquals(expected, result, "Pattern: " + pattern + ", MimeType: " + mimeType);
    }

    @Test
    @SneakyThrows
    void testInstantiateHandler() {
        AngusMailActivation activation = new AngusMailActivation();
        Method method = AngusMailActivation.class.getDeclaredMethod("instantiateHandler", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Optional<DataContentHandler> handler = (Optional<DataContentHandler>) method.invoke(
            activation, "org.eclipse.angus.mail.handlers.text_plain"
        );

        assertTrue(handler.isPresent());
        assertEquals("org.eclipse.angus.mail.handlers.text_plain", handler.get().getClass().getName());
    }

    @Test
    @SneakyThrows
    void testInstantiateHandlerNotFound() {
        AngusMailActivation activation = new AngusMailActivation();
        Method method = AngusMailActivation.class.getDeclaredMethod("instantiateHandler", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Optional<DataContentHandler> handler = (Optional<DataContentHandler>) method.invoke(
            activation, "non.existent.Handler"
        );

        assertFalse(handler.isPresent());
    }

    @ParameterizedTest
    @CsvSource(
        {
            "text/plain, org.eclipse.angus.mail.handlers.text_plain",
            "text/plain; charset=utf-8, org.eclipse.angus.mail.handlers.text_plain",
            "text/html, org.eclipse.angus.mail.handlers.text_html",
            "text/xml, org.eclipse.angus.mail.handlers.text_xml",
            "multipart/mixed, org.eclipse.angus.mail.handlers.multipart_mixed",
            "multipart/alternative, org.eclipse.angus.mail.handlers.multipart_mixed",
            "message/rfc822, org.eclipse.angus.mail.handlers.message_rfc822",
            "application/pdf, 'EMPTY'"
        }
    )
    @SneakyThrows
    void testFindHandlerClassName(String mimeType, String expectedClassName) {
        AngusMailActivation activation = new AngusMailActivation();
        Method method = AngusMailActivation.class.getDeclaredMethod("findHandlerClassName", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Optional<String> result = (Optional<String>) method.invoke(activation, mimeType);

        if (expectedClassName.equals("EMPTY")) {
            assertFalse(result.isPresent());
        } else {
            assertTrue(result.isPresent());
            assertEquals(expectedClassName, result.get());
        }
    }
}
