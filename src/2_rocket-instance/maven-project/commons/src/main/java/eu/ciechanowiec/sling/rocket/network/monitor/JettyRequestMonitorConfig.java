package eu.ciechanowiec.sling.rocket.network.monitor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link SlingRequestMonitor}.
 */
@ObjectClassDefinition
public @interface JettyRequestMonitorConfig {

    /**
     * Indicates whether the configured service is enabled or disabled.
     *
     * @return {@code true} if the configured service is enabled; {@code false} otherwise
     */
    @AttributeDefinition(
        name = "Enabled?",
        description = "Indicates whether the configured service is enabled or disabled",
        type = AttributeType.BOOLEAN
    )
    @SuppressWarnings("squid:S100")
    boolean is$_$enabled() default false;
}
