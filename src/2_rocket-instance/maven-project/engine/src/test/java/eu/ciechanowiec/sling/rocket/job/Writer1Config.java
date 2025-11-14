package eu.ciechanowiec.sling.rocket.job;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface Writer1Config {

    @AttributeDefinition(
        name = "Quartz Cron Expression",
        description = "Determines the regularity of a recurring task",
        defaultValue = "0 * * * * ?", // Every minute
        type = AttributeType.STRING
    )
    @SuppressWarnings({"squid:S100", "squid:S125"})
    String schedule$_$cycle_cron$_$expression() default "0 * * * * ?"; // Every minute
}
