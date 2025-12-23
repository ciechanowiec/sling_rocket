package eu.ciechanowiec.sling.rocket.identity.mapping;

import eu.ciechanowiec.sling.rocket.job.SchedulableJobConsumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * {@link ObjectClassDefinition} for {@link GroupToAuthMapping}.
 */
@ObjectClassDefinition
public @interface GroupToAuthMappingConfig {

    /**
     * Mappings of {@link Group}s to {@link Authorizable}s. The syntax of every mapping is the following:
     * {@code <name-of-group>###<set-of-authorizable-ids>} ({@code ###} is a delimiter). Elements of every
     * {@code <set-of-authorizable-ids>} are separated by {@code <<<>>>}.
     *
     * @return mappings of {@link Group}s to {@link Authorizable}s
     */
    @AttributeDefinition(
        name = "Groups to Authorizables Mappings",
        description = "Mappings of Groups to Authorizables. The syntax of every mapping is the following: "
            + "<name-of-group>###<set-of-authorizable-ids> (### is a delimiter). "
            + "Elements of every <set-of-authorizable-ids> are separated by <<<>>>",
        defaultValue = StringUtils.EMPTY,
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String[] groups$_$to$_$auths_mappings() default {""};

    /**
     * Value for the {@link SchedulableJobConsumer#CRON_EXPRESSION_PROPERTY} that determines the schedule cycle of how
     * often the {@link GroupToAuthMapping#mapAll()} is executed. By default, the value is an empty {@link String} which
     * means that the {@link GroupToAuthMapping#mapAll()} is not executed periodically.
     *
     * @return value for the {@link SchedulableJobConsumer#CRON_EXPRESSION_PROPERTY} that determines the schedule cycle
     * of how often the {@link GroupToAuthMapping#mapAll()} is executed
     */
    @AttributeDefinition(
        name = "Quartz Cron Expression",
        description = "Determines the regularity of a recurring task",
        defaultValue = "",
        type = AttributeType.STRING
    )
    @SuppressWarnings({"squid:S100", "squid:S125"})
    String schedule$_$cycle_cron$_$expression() default "";
}
