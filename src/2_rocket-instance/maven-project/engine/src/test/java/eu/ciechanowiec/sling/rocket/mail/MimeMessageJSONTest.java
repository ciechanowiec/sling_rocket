package eu.ciechanowiec.sling.rocket.mail;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Header;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings(
    {
        "ClassWithTooManyMethods", "PMD.ReplaceJavaUtilDate", "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals",
        "PMD.LinguisticNaming", "MethodCount", "MultipleStringLiterals"
    }
)
class MimeMessageJSONTest {

    private MimeMessage mimeMessage;
    private MimeMessageJSON mimeMessageJSON;
    private Session session;

    @BeforeEach
    void setup() {
        session = Session.getDefaultInstance(new Properties());
        mimeMessage = new MimeMessage(session);
        mimeMessageJSON = new MimeMessageJSON(mimeMessage);
    }

    @SneakyThrows
    @Test
    void subjectShouldReturnSubject() {
        String expectedSubject = "Test Subject";
        mimeMessage.setSubject(expectedSubject);
        Optional<String> result = mimeMessageJSON.subject();
        assertTrue(result.isPresent());
        assertEquals(expectedSubject, result.get());
    }

    @Test
    void subjectShouldReturnEmptyWhenNull() {
        Optional<String> result = mimeMessageJSON.subject();
        assertTrue(result.isEmpty());
    }

    @Test
    void subjectShouldReturnEmptyOnMessagingException() {
        MimeMessage exceptionMessage = new MimeMessage(session) {

            @SneakyThrows
            @Override
            public String getSubject() {
                throw new MessagingException("Simulated error");
            }
        };
        MimeMessageJSON messageJSON = new MimeMessageJSON(exceptionMessage);
        Optional<String> result = messageJSON.subject();
        assertTrue(result.isEmpty());
    }

    @SneakyThrows
    @Test
    void fromShouldReturnSenders() {
        InternetAddress[] senders = {new InternetAddress("sender@example.com")};
        mimeMessage.addFrom(senders);
        List<String> result = mimeMessageJSON.from();
        assertEquals(1, result.size());
        assertEquals(senders[0].toString(), result.getFirst());
    }

    @Test
    void fromShouldReturnEmptyWhenNoSenders() {
        List<String> result = mimeMessageJSON.from();
        assertTrue(result.isEmpty());
    }

    @Test
    void fromShouldReturnEmptyOnMessagingException() {
        MimeMessage exceptionMessage = new MimeMessage(session) {

            @SneakyThrows
            @Override
            public Address[] getFrom() {
                throw new MessagingException("Simulated error");
            }
        };
        MimeMessageJSON messageJSON = new MimeMessageJSON(exceptionMessage);
        List<String> result = messageJSON.from();
        assertTrue(result.isEmpty());
    }

    @SneakyThrows
    @Test
    void toShouldReturnRecipients() {
        InternetAddress[] recipients = {new InternetAddress("to@example.com")};
        mimeMessage.setRecipients(Message.RecipientType.TO, recipients);
        List<String> result = mimeMessageJSON.to();
        assertEquals(1, result.size());
        assertEquals(recipients[0].toString(), result.getFirst());
    }

    @SneakyThrows
    @Test
    void toShouldReturnMultipleRecipients() {
        InternetAddress[] recipients = {
            new InternetAddress("to1@example.com"),
            new InternetAddress("to2@example.com")
        };
        mimeMessage.setRecipients(Message.RecipientType.TO, recipients);
        List<String> result = mimeMessageJSON.to();
        assertEquals(2, result.size());
        assertEquals(recipients[0].toString(), result.get(0));
        assertEquals(recipients[1].toString(), result.get(1));
    }

    @Test
    void toShouldReturnEmptyOnMessagingException() {
        MimeMessage exceptionMessage = new MimeMessage(session) {

            @SneakyThrows
            @Override
            public Address[] getRecipients(Message.RecipientType type) {
                throw new MessagingException("Simulated error");
            }
        };
        MimeMessageJSON messageJSON = new MimeMessageJSON(exceptionMessage);
        List<String> result = messageJSON.to();
        assertTrue(result.isEmpty());
    }

    @SneakyThrows
    @Test
    void ccShouldReturnRecipients() {
        InternetAddress[] recipients = {new InternetAddress("cc@example.com")};
        mimeMessage.setRecipients(Message.RecipientType.CC, recipients);
        List<String> result = mimeMessageJSON.cc();
        assertEquals(1, result.size());
        assertEquals(recipients[0].toString(), result.getFirst());
    }

    @SneakyThrows
    @Test
    void bccShouldReturnRecipients() {
        InternetAddress[] recipients = {new InternetAddress("bcc@example.com")};
        mimeMessage.setRecipients(Message.RecipientType.BCC, recipients);
        List<String> result = mimeMessageJSON.bcc();
        assertEquals(1, result.size());
        assertEquals(recipients[0].toString(), result.getFirst());
    }

    @SneakyThrows
    @Test
    void replyToShouldReturnAddresses() {
        InternetAddress[] addresses = {new InternetAddress("replyto@example.com")};
        mimeMessage.setReplyTo(addresses);
        List<String> result = mimeMessageJSON.replyTo();
        assertEquals(1, result.size());
        assertEquals(addresses[0].toString(), result.getFirst());
    }

    @Test
    void replyToShouldReturnEmptyOnMessagingException() {
        MimeMessage exceptionMessage = new MimeMessage(session) {

            @SneakyThrows
            @Override
            public Address[] getReplyTo() {
                throw new MessagingException("Simulated error");
            }
        };
        MimeMessageJSON messageJSON = new MimeMessageJSON(exceptionMessage);
        List<String> result = messageJSON.replyTo();
        assertTrue(result.isEmpty());
    }

    @SneakyThrows
    @Test
    void sentDateShouldReturnDate() {
        Date date = new Date();
        // Truncate to seconds because MIME dates don't have sub-second precision
        // and MimeMessage might truncate it when storing in headers
        long truncatedTime = date.getTime() / 1000 * 1000;
        date = new Date(truncatedTime);
        mimeMessage.setSentDate(date);
        Optional<LocalDateTime> result = mimeMessageJSON.sentDate();
        assertTrue(result.isPresent());
        LocalDateTime expected = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        assertEquals(expected, result.get());
    }

    @Test
    void sentDateShouldReturnEmptyOnMessagingException() {
        MimeMessage exceptionMessage = new MimeMessage(session) {

            @SneakyThrows
            @Override
            public Date getSentDate() {
                throw new MessagingException("Simulated error");
            }
        };
        MimeMessageJSON messageJSON = new MimeMessageJSON(exceptionMessage);
        Optional<LocalDateTime> result = messageJSON.sentDate();
        assertTrue(result.isEmpty());
    }

    @SneakyThrows
    @Test
    void contentShouldReturnStringContent() {
        String content = "Hello World";
        mimeMessage.setText(content);
        Optional<String> result = mimeMessageJSON.content();
        assertTrue(result.isPresent());
        assertEquals(content, result.get());
    }

    @SneakyThrows
    @Test
    void contentShouldHandleMultipart() {
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Part 1");
        multipart.addBodyPart(textPart);
        MimeBodyPart textPart2 = new MimeBodyPart();
        textPart2.setText("Part 2");
        multipart.addBodyPart(textPart2);
        mimeMessage.setContent(multipart);
        Optional<String> result = mimeMessageJSON.content();
        assertTrue(result.isPresent());
        assertEquals("Part 1Part 2", result.get());
    }

    @SneakyThrows
    @Test
    void contentShouldIgnoreAttachments() {
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Text Part");
        multipart.addBodyPart(textPart);
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setDisposition(BodyPart.ATTACHMENT);
        attachmentPart.setText("Attachment Part");
        multipart.addBodyPart(attachmentPart);
        mimeMessage.setContent(multipart);
        Optional<String> result = mimeMessageJSON.content();
        assertTrue(result.isPresent());
        assertEquals("Text Part", result.get());
    }

    @SneakyThrows
    @Test
    void contentShouldHandleEmptyMultipart() {
        MimeMultipart multipart = new MimeMultipart();
        mimeMessage.setContent(multipart);
        Optional<String> result = mimeMessageJSON.content();
        assertTrue(result.isPresent());
        assertEquals("", result.get());
    }

    @SneakyThrows
    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void contentShouldHandleNestedMultipart() {
        MimeMultipart outer = new MimeMultipart();
        MimeMultipart inner = new MimeMultipart();
        MimeBodyPart innerPart = new MimeBodyPart();
        innerPart.setText("Inner Text");
        inner.addBodyPart(innerPart);
        MimeBodyPart nestedPart = new MimeBodyPart();
        nestedPart.setContent(inner);
        outer.addBodyPart(nestedPart);
        mimeMessage.setContent(outer);
        Optional<String> result = mimeMessageJSON.content();
        assertTrue(result.isPresent());
        assertEquals("Inner Text", result.get());
    }

    @SneakyThrows
    @Test
    @SuppressWarnings({"PMD.UseUnderscoresInNumericLiterals", "MagicNumber"})
    void contentShouldHandleOtherObjectTypes() {
        mimeMessage.setContent(12345, "text/plain");
        Optional<String> result = mimeMessageJSON.content();
        assertTrue(result.isPresent());
        assertEquals("12345", result.get());
    }

    @Test
    @SuppressWarnings("Regexp")
    void contentShouldHandleNull() {
        MimeMessage nullContentMessage = new MimeMessage(session) {

            @Override
            public @Nullable Object getContent() {
                return null;
            }
        };
        MimeMessageJSON messageJSON = new MimeMessageJSON(nullContentMessage);
        Optional<String> result = messageJSON.content();
        assertTrue(result.isPresent());
        assertEquals("", result.get());
    }

    @Test
    void contentShouldReturnEmptyOnMessagingException() {
        MimeMessage exceptionMessage = new MimeMessage(session) {

            @SneakyThrows
            @Override
            public Object getContent() {
                throw new MessagingException("Simulated error");
            }
        };
        MimeMessageJSON messageJSON = new MimeMessageJSON(exceptionMessage);
        Optional<String> result = messageJSON.content();
        assertTrue(result.isEmpty());
    }

    @Test
    void contentShouldReturnEmptyOnIOException() {
        MimeMessage exceptionMessage = new MimeMessage(session) {

            @Override
            public Object getContent() throws IOException {
                throw new IOException("Simulated error");
            }
        };
        MimeMessageJSON messageJSON = new MimeMessageJSON(exceptionMessage);
        Optional<String> result = messageJSON.content();
        assertTrue(result.isEmpty());
    }

    @SneakyThrows
    @Test
    void contentShouldHandleExceptionInBodyPart() {
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Good Part");
        multipart.addBodyPart(textPart);
        MimeBodyPart badPart = new MimeBodyPart() {

            @Override
            public Object getContent() throws IOException {
                throw new IOException("Bad part content");
            }
        };
        multipart.addBodyPart(badPart);
        mimeMessage.setContent(multipart);
        Optional<String> result = mimeMessageJSON.content();
        assertTrue(result.isPresent());
        assertEquals("Good Part", result.get());
    }

    @SneakyThrows
    @Test
    void headersShouldReturnMap() {
        mimeMessage.addHeader("X-Header-1", "Value 1");
        mimeMessage.addHeader("X-Header-2", "Value 2");
        mimeMessage.addHeader("X-Header-1", "Value 3");
        Map<String, List<String>> result = mimeMessageJSON.headers();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("X-Header-1"));
        assertEquals(List.of("Value 1", "Value 3"), result.get("X-Header-1"));
        assertTrue(result.containsKey("X-Header-2"));
        assertEquals(List.of("Value 2"), result.get("X-Header-2"));
    }

    @Test
    void headersShouldReturnEmptyOnMessagingException() {
        MimeMessage exceptionMessage = new MimeMessage(session) {

            @SneakyThrows
            @Override
            public Enumeration<Header> getAllHeaders() {
                throw new MessagingException("Simulated error");
            }
        };
        MimeMessageJSON messageJSON = new MimeMessageJSON(exceptionMessage);
        Map<String, List<String>> result = messageJSON.headers();
        assertTrue(result.isEmpty());
    }

    @SneakyThrows
    @Test
    void asJSONShouldReturnValidJSON() {
        mimeMessage.setSubject("Test Subject");
        mimeMessage.setSentDate(new Date(0)); // 1970-01-01T00:00:00Z
        String json = mimeMessageJSON.asJSON();
        assertTrue(json.contains("\"subject\":\"Test Subject\""));
        // Date format depends on the system default timezone as per the implementation in MimeMessageJSON,
        // so we just check if it contains the sentDate field
        assertTrue(json.contains("\"sentDate\":"));
    }

    @SneakyThrows
    @Test
    void toStringShouldCallAsJSON() {
        mimeMessage.setSubject("Test Subject");
        String json = mimeMessageJSON.asJSON();
        assertEquals(json, mimeMessageJSON.toString());
    }
}
