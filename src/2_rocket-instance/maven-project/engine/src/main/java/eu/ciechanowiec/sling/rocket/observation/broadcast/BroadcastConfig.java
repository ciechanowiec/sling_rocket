package eu.ciechanowiec.sling.rocket.observation.broadcast;

import org.apache.sling.commons.messaging.mail.internal.SimpleMailService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.quartz.CronExpression;

/**
 * Configuration for {@link Broadcast}.
 */
@SuppressWarnings("WeakerAccess")
@ObjectClassDefinition
public @interface BroadcastConfig {

    /**
     * Email addresses of the recipients to which the application statistics should be broadcasted. If empty, the
     * statistics should not be broadcasted to any email address.
     *
     * @return email addresses of the recipients to which the application statistics should be broadcasted
     */
    @AttributeDefinition(
        name = "Recipient Emails",
        description = "Email addresses of the recipients to which the application statistics should be broadcasted. "
            + "If empty, the statistics should not be broadcasted to any email address",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String[] to_emails() default {};

    /**
     * Email address of the sender from which the application statistics should be broadcasted.
     * <p>
     * The value must be the same as the value of the {@code mail.smtps.from} {@link Component#property()} of the
     * {@link SimpleMailService} used by the {@link Broadcast} for sending emails.
     *
     * @return email address of the sender from which the application statistics should be broadcasted
     */
    @AttributeDefinition(
        name = "Sender Email",
        description = "Email address of the sender from which the application statistics should be broadcasted. "
            + "The value must be the same as the value of the 'mail.smtps.from' OSGi property of the "
            + "SimpleMailService used by the Broadcast for sending emails",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String from_email();

    /**
     * Name of the sender from which the application statistics should be broadcasted.
     *
     * @return name of the sender from which the application statistics should be broadcasted
     */
    @AttributeDefinition(
        name = "Sender Name",
        description = "Name of the sender from which the application statistics should be broadcasted",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String from_name();

    /**
     * Subject of an email in which the application statistics will be broadcasted.
     *
     * @return subject of an email in which the application statistics will be broadcasted
     */
    @AttributeDefinition(
        name = "Email Subject",
        description = "Subject of an email in which the application statistics will be broadcasted",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String email$_$subject();

    /**
     * Quartz {@link CronExpression} that determines the schedule of the broadcast cycle; for example,
     * {@code 0 * * * * ?} means that the broadcast cycle will be executed every minute.
     *
     * @return Quartz {@link CronExpression} that determines the schedule of the broadcast cycle
     */
    @AttributeDefinition(
        name = "Broadcast Cron Schedule Cycle",
        description = "Quartz cron expression that determines the schedule of the broadcast cycle; "
            + "for example, '0 * * * * ?' means that the broadcast cycle will be executed every minute",
        type = AttributeType.STRING
    )
    @SuppressWarnings({"squid:S100", "squid:S125"})
    String schedule$_$cycle_cron$_$expression();
}
