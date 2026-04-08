package eu.ciechanowiec.sling.rocket.observation.broadcast;

import eu.ciechanowiec.sling.rocket.observation.audit.EntryTrampoline;
import eu.ciechanowiec.sling.rocket.observation.stats.RocketStatsDisplay;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;
import org.apache.sling.commons.messaging.mail.MailService;
import org.apache.sling.commons.messaging.mail.MessageBuilder;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("MultipleStringLiterals")
class BroadcastTest extends TestEnvironment {

    private MailService mailService;
    private MessageBuilder messageBuilder;
    private JobManager jobManager;
    private Broadcast broadcast;
    private EntryTrampoline entryTrampoline;

    BroadcastTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @SneakyThrows
    @BeforeEach
    void setup() {
        mailService = mock(MailService.class);
        messageBuilder = mock(MessageBuilder.class);
        jobManager = mock(JobManager.class);

        when(mailService.getMessageBuilder()).thenReturn(messageBuilder);
        when(messageBuilder.to(any(String[].class))).thenReturn(messageBuilder);
        when(messageBuilder.from(anyString(), anyString())).thenReturn(messageBuilder);
        when(messageBuilder.subject(anyString())).thenReturn(messageBuilder);
        when(messageBuilder.html(anyString())).thenReturn(messageBuilder);
        when(messageBuilder.attachment(any(byte[].class), anyString(), anyString())).thenReturn(messageBuilder);

        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);
        when(messageBuilder.build()).thenReturn(mimeMessage);

        lenient().when(mailService.sendMessage(any(MimeMessage.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        context.registerService(MailService.class, mailService);
        context.registerService(JobManager.class, jobManager);

        context.registerInjectActivateService(RocketStatsDisplay.class);
        entryTrampoline = context.registerInjectActivateService(EntryTrampoline.class, Map.of("is-enabled", true));

        broadcast = context.registerInjectActivateService(
            Broadcast.class, Map.of(
                "to.emails", new String[]{"recipient@example.com"},
                "from.email", "sender@example.com",
                "from.name", "Sender Name",
                "email-subject", "Subject",
                "schedule-cycle.cron-expression", "0 * * * * ?"
            )
        );
    }

    @Test
    @SneakyThrows
    void testBroadcast() {
        Optional<String> result = broadcast.broadcast();

        assertTrue(result.isPresent());
        String statsJson = result.get();
        assertTrue(statsJson.contains("rocketStats"));

        verify(mailService).sendMessage(any(MimeMessage.class));
        verify(messageBuilder).to(
            argThat(
                (String[] emails) -> Objects.nonNull(emails)
                    && emails.length == 1
                    && "recipient@example.com".equals(emails[0])
            )
        );
        verify(messageBuilder).from("sender@example.com", "Sender Name");
        verify(messageBuilder).subject("Subject");

        // Verify that an entry was submitted to the trampoline and then to JobManager
        entryTrampoline.deactivate(); // Triggers flush
        verify(jobManager).addJob(eq("eu/ciechanowiec/sling/rocket/observation/audit/STORAGE"), anyMap());
    }

    @Test
    void testProcessJob() {
        Job job = mock(Job.class);
        JobConsumer.JobResult result = broadcast.process(job);

        assertEquals(JobConsumer.JobResult.OK, result);
        verify(mailService).sendMessage(any(MimeMessage.class));
    }

    @Test
    void testBroadcastEmailSendingFailure() {
        reset(mailService);
        when(mailService.getMessageBuilder()).thenReturn(messageBuilder);
        CompletableFuture<Void> failureFuture = new CompletableFuture<>();
        failureFuture.completeExceptionally(new RuntimeException("Email sending failed"));
        when(mailService.sendMessage(any(MimeMessage.class))).thenReturn(failureFuture);

        Optional<String> result = broadcast.broadcast();

        // broadcast() joins the future and logs error, but still returns stats JSON if buildBroadcastMessage succeeded
        assertTrue(result.isPresent());
        verify(mailService).sendMessage(any(MimeMessage.class));

        // entryTrampoline.submitForSaving is NOT called in thenAccept because it's only called on success
        entryTrampoline.deactivate();
        verify(jobManager, never()).addJob(eq("eu/ciechanowiec/sling/rocket/observation/audit/STORAGE"), anyMap());
    }

    @SneakyThrows
    @Test
    void testBroadcastMessagingException() {
        reset(messageBuilder);
        when(messageBuilder.to(any(String[].class))).thenReturn(messageBuilder);
        when(messageBuilder.from(anyString(), anyString())).thenReturn(messageBuilder);
        when(messageBuilder.subject(anyString())).thenReturn(messageBuilder);
        when(messageBuilder.html(anyString())).thenReturn(messageBuilder);
        when(messageBuilder.attachment(any(byte[].class), anyString(), anyString())).thenReturn(messageBuilder);

        when(messageBuilder.build()).thenThrow(new MessagingException("Simulated exception"));

        Optional<String> result = broadcast.broadcast();
        assertTrue(result.isEmpty());
    }
}
