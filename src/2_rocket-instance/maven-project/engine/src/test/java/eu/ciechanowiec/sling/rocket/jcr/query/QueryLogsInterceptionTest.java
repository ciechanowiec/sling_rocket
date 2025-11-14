package eu.ciechanowiec.sling.rocket.jcr.query;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("PMD.MoreThanOneLogger")
class QueryLogsInterceptionTest extends TestEnvironment {

    private QueryLogsInterception queryLogsInterception;
    private Logger oakQueryLogger;
    private Logger otherLogger;

    QueryLogsInterceptionTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @BeforeEach
    void setup() {
        queryLogsInterception = context.registerInjectActivateService(QueryLogsInterception.class);
        oakQueryLogger = (Logger) LoggerFactory.getLogger("org.apache.jackrabbit.oak.query.SQL2Parser");
        otherLogger = (Logger) LoggerFactory.getLogger("eu.ciechanowiec.sling.rocket.other.SomeClass");
    }

    @AfterEach
    void teardown() {
        MDC.clear();
    }

    @Test
    void shouldInterceptWhenKeyPresentAndLoggerMatches() {
        String interceptionKey = UUID.randomUUID().toString();
        MDC.put(QueryLogsInterception.INTERCEPTION_KEY_NAME, interceptionKey);
        String message = "This is a test message.";

        FilterReply reply = queryLogsInterception.decide(null, oakQueryLogger, Level.INFO, message, null, null);

        List<String> savedLogs = queryLogsInterception.savedILoggingEvents(interceptionKey);
        assertAll(
            () -> assertEquals(FilterReply.NEUTRAL, reply),
            () -> assertEquals(1, savedLogs.size()),
            () -> assertEquals(message + "\n", savedLogs.getFirst())
        );
    }

    @Test
    void shouldNotInterceptWhenKeyIsMissing() {
        String interceptionKey = UUID.randomUUID().toString();
        String message = "This message should not be intercepted.";

        queryLogsInterception.decide(null, oakQueryLogger, Level.INFO, message, null, null);

        List<String> savedLogs = queryLogsInterception.savedILoggingEvents(interceptionKey);
        assertTrue(savedLogs.isEmpty());
    }

    @Test
    void shouldFilterOutNullFormattedMessage() {
        String interceptionKey = UUID.randomUUID().toString();
        MDC.put(QueryLogsInterception.INTERCEPTION_KEY_NAME, interceptionKey);
        String validMessage = "A valid message";

        queryLogsInterception.decide(null, oakQueryLogger, Level.INFO, null, null, null);
        queryLogsInterception.decide(null, oakQueryLogger, Level.INFO, validMessage, null, null);

        List<String> savedLogs = queryLogsInterception.savedILoggingEvents(interceptionKey);

        assertAll(
            () -> assertEquals(1, savedLogs.size()),
            () -> assertEquals(validMessage + "\n", savedLogs.getFirst())
        );
    }

    @Test
    void shouldNotInterceptWhenLoggerDoesNotMatch() {
        String interceptionKey = UUID.randomUUID().toString();
        MDC.put(QueryLogsInterception.INTERCEPTION_KEY_NAME, interceptionKey);
        String message = "This message is from a non-matching logger.";

        queryLogsInterception.decide(null, otherLogger, Level.INFO, message, null, null);

        List<String> savedLogs = queryLogsInterception.savedILoggingEvents(interceptionKey);
        assertTrue(savedLogs.isEmpty());
    }

    @Test
    void shouldCorrectlyFormatMessageWithParameters() {
        String interceptionKey = UUID.randomUUID().toString();
        MDC.put(QueryLogsInterception.INTERCEPTION_KEY_NAME, interceptionKey);
        String format = "Message with parameter: {}";
        Object[] params = {"param-value"};

        queryLogsInterception.decide(null, oakQueryLogger, Level.INFO, format, params, null);

        List<String> savedLogs = queryLogsInterception.savedILoggingEvents(interceptionKey);
        assertEquals(1, savedLogs.size());
        assertEquals("Message with parameter: param-value\n", savedLogs.getFirst());
    }

    @Test
    @SuppressWarnings("MagicNumber")
    void shouldAdhereToMessageCountLimit() {
        String interceptionKey = UUID.randomUUID().toString();
        MDC.put(QueryLogsInterception.INTERCEPTION_KEY_NAME, interceptionKey);
        int messageLimit = 500;

        for (int i = 0; i < messageLimit + 10; i++) {
            queryLogsInterception.decide(null, oakQueryLogger, Level.INFO, "Message " + i, null, null);
        }

        List<String> savedLogs = queryLogsInterception.savedILoggingEvents(interceptionKey);
        assertEquals(messageLimit, savedLogs.size());
        assertEquals("Message 499\n", savedLogs.get(messageLimit - 1));
    }

    @Test
    void shouldRemoveSavedLogs() {
        String interceptionKey = UUID.randomUUID().toString();
        MDC.put(QueryLogsInterception.INTERCEPTION_KEY_NAME, interceptionKey);
        queryLogsInterception.decide(null, oakQueryLogger, Level.INFO, "A message", null, null);

        assertFalse(queryLogsInterception.savedILoggingEvents(interceptionKey).isEmpty());

        queryLogsInterception.stopInterception(interceptionKey);

        assertTrue(queryLogsInterception.savedILoggingEvents(interceptionKey).isEmpty());
    }

    @Test
    void shouldReturnEmptyListForUnknownKey() {
        List<String> savedLogs = queryLogsInterception.savedILoggingEvents("non-existent-key");
        assertTrue(savedLogs.isEmpty());
    }
}
