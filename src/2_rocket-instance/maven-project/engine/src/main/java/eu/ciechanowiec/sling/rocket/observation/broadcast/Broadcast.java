package eu.ciechanowiec.sling.rocket.observation.broadcast;

import eu.ciechanowiec.sling.rocket.job.SchedulableJobConsumer;
import eu.ciechanowiec.sling.rocket.mail.MimeMessageJSON;
import eu.ciechanowiec.sling.rocket.observation.audit.Entry;
import eu.ciechanowiec.sling.rocket.observation.audit.EntryTrampoline;
import eu.ciechanowiec.sling.rocket.observation.stats.RocketStatsDisplay;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.MediaType;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.apache.sling.commons.messaging.mail.MailService;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.jspecify.annotations.Nullable;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Broadcasts application statistics.
 */
@Component(
    service = {JobConsumer.class, SchedulableJobConsumer.class, Broadcast.class, BroadcastMBean.class},
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + "eu/ciechanowiec/sling/rocket/observation/broadcast/BROADCAST",
        "jmx.objectname=eu.ciechanowiec.sling.rocket.engine:type=Observation,name=Broadcast"
    }
)
@Slf4j
@ServiceDescription(Broadcast.SERVICE_DESCRIPTION)
@Designate(ocd = BroadcastConfig.class)
@ToString
public class Broadcast extends AnnotatedStandardMBean implements BroadcastMBean, SchedulableJobConsumer {

    static final String SERVICE_DESCRIPTION = "Broadcasts application statistics";

    @ToString.Exclude
    private final MailService mailService;
    @ToString.Exclude
    private final RocketStatsDisplay rocketStatsDisplay;
    private final AtomicReference<BroadcastConfig> config;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @ToString.Exclude
    private final Optional<EntryTrampoline> entryTrampoline;

    /**
     * Constructs an instance of this class.
     *
     * @param mailService        {@link MailService} used for sending emails to broadcast application statistics
     * @param rocketStatsDisplay {@link RocketStatsDisplay} used for obtaining application statistics to be broadcasted
     * @param entryTrampoline    {@link EntryTrampoline} used for saving an {@link Entry} that represents a broadcasted
     *                           application statistics; if not available, the {@link Entry} will be only logged and not
     *                           saved
     * @param config             {@link BroadcastConfig} used by the constructed instance
     */
    @Activate
    public Broadcast(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        MailService mailService,
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        RocketStatsDisplay rocketStatsDisplay,
        @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY
        )
        @Nullable
        EntryTrampoline entryTrampoline,
        BroadcastConfig config
    ) {
        super(BroadcastMBean.class);
        this.mailService = mailService;
        this.rocketStatsDisplay = rocketStatsDisplay;
        this.config = new AtomicReference<>(config);
        this.entryTrampoline = Optional.ofNullable(entryTrampoline);
        log.info("Initialized {}", this);
    }

    @Modified
    void configure(
        BroadcastConfig config
    ) {
        this.config.set(config);
        log.info("Reconfigured {}", this);
    }

    @Override
    @SuppressWarnings("Regexp")
    public Optional<String> broadcast() {
        log.debug("Broadcasting");
        String rocketStatsJSON = rocketStatsDisplay.asJSON();
        try {
            MimeMessage broadcastMessage = buildBroadcastMessage(rocketStatsJSON);
            mailService.sendMessage(broadcastMessage)
                .thenAccept(
                    _ -> {
                        Entry entry = toEntry(broadcastMessage, rocketStatsJSON);
                        entryTrampoline.ifPresentOrElse(
                            entryTrampolineNonNull -> {
                                entryTrampolineNonNull.submitForSaving(entry);
                                log.debug("Broadcast sent");
                            },
                            () -> log.debug("Broadcast sent: {}", entry)
                        );
                    }
                ).exceptionally(
                    exception -> {
                        log.error("Failed to send email for {}", broadcastMessage, exception);
                        return null;
                    }
                ).join();
            return Optional.of(rocketStatsJSON);
        } catch (MessagingException exception) {
            log.error("Failed to broadcast", exception);
            return Optional.empty();
        }
    }

    private Entry toEntry(MimeMessage mimeMessage, String rocketStatsJSON) {
        String mimeMessageJSON = new MimeMessageJSON(mimeMessage).asJSON();
        return new Entry(
            "system", Broadcast.class.getName(), LocalDateTime.now(),
            Map.of("mimeMessageJSON", mimeMessageJSON, "rocketStatsJSON", rocketStatsJSON)
        );
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private MimeMessage buildBroadcastMessage(String rocketStatsJSON) throws MessagingException {
        return mailService.getMessageBuilder()
            .to(config.get().to_emails())
            .from(config.get().from_email(), config.get().from_name())
            .subject(config.get().email$_$subject())
            .html(htmlContent())
            .attachment(
                rocketStatsJSON.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON,
                "rocket-stats-%s.json".formatted(LocalDateTime.now())
            ).build();
    }

    @SuppressWarnings("LineLength")
    private String htmlContent() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Sling Rocket - Observation Stats</title>
                <style>
                    body { margin: 0; padding: 0; background-color: #f4f7f6; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; color: #333333; line-height: 1.6; }
                    .container { max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05); border: 1px solid #e1e4e8; }
                    .header { background-color: #24292e; color: #ffffff; padding: 20px 30px; border-bottom: 3px solid #2ea44f; }
                    .header h1 { margin: 0; font-size: 20px; font-weight: 600; display: flex; align-items: center; }
                    .content { padding: 30px; }
                    .content p { margin: 0 0 15px 0; font-size: 15px; color: #24292e; }
                    .attachment-box { background-color: #f6f8fa; border: 1px dashed #d1d5da; border-radius: 6px; padding: 15px; margin: 25px 0; text-align: center; }
                    .attachment-box span { font-weight: 600; color: #0366d6; font-size: 14px; }
                    .footer { background-color: #fafbfc; padding: 20px 30px; text-align: center; font-size: 12px; color: #6a737d; border-top: 1px solid #e1e4e8; }
                    .footer a { color: #0366d6; text-decoration: none; }
                    .footer a:hover { text-decoration: underline; }
                </style>
            </head>
            <body>
            <div class="container" style="max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; font-family: sans-serif; border: 1px solid #e1e4e8;">

                <div class="header" style="background-color: #24292e; color: #ffffff; padding: 20px 30px; border-bottom: 3px solid #2ea44f;">
                    <h1 style="margin: 0; font-size: 20px;">🚀 Sling Rocket Stats</h1>
                </div>

                <div class="content" style="padding: 30px;">
                    <p style="margin: 0 0 15px 0; font-size: 15px; color: #24292e;">The latest automated observation stats for your Sling Rocket instance have been generated.</p>
                    <p style="margin: 0 0 15px 0; font-size: 15px; color: #24292e;">Please find the JSON file containing the comprehensive system metrics attached to this email.</p>
                </div>

                <div class="footer" style="background-color: #fafbfc; padding: 20px 30px; text-align: center; font-size: 12px; color: #6a737d; border-top: 1px solid #e1e4e8;">
                    This is an automated report generated by <a href="https://github.com/ciechanowiec/sling_rocket" target="_blank" style="color: #0366d6; text-decoration: none;">Sling Rocket</a>.<br>
                    Please do not reply directly to this email.
                </div>
            </div>
            </body>
            </html>""";
    }

    @Override
    public JobResult process(Job job) {
        log.info("Scheduled broadcast triggered");
        return broadcast().map(
            _ -> JobResult.OK
        ).orElseGet(
            () -> {
                log.warn("Failed to send broadcast email");
                return JobResult.FAILED;
            }
        );
    }
}
