package eu.ciechanowiec.sling.rocket.clamav;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.net.Socket;

/**
 * Configuration that describes how the associated {@link ClamAV} should connect to a ClamAV daemon (clamd).
 * <p>
 * All properties have defaults that match the standard <i>Sling Rocket</i> deployment, where the SR ClamAV listens at
 * {@code rocket-clamav:3310}, so the associated {@link ClamAV} is fully functional without any explicit configuration.
 */
@ObjectClassDefinition
public @interface ClamAVConfig {

    /**
     * Host of the ClamAV daemon (clamd) to connect to.
     *
     * @return host of the ClamAV daemon (clamd) to connect to
     */
    @AttributeDefinition(
        name = "ClamAV Host",
        description = "Host of the ClamAV daemon (clamd) to connect to.",
        defaultValue = "rocket-clamav",
        type = AttributeType.STRING
    )
    String clamav_host() default "rocket-clamav";

    /**
     * TCP port of the ClamAV daemon (clamd) to connect to.
     *
     * @return TCP port of the ClamAV daemon (clamd) to connect to
     */
    @SuppressWarnings("MagicNumber")
    @AttributeDefinition(
        name = "ClamAV Port",
        description = "TCP port of the ClamAV daemon (clamd) to connect to.",
        defaultValue = "3310",
        type = AttributeType.INTEGER,
        min = "1",
        max = "65535"
    )
    int clamav_port() default 3310;

    /**
     * Maximum time in milliseconds to wait for a {@link Socket} connection with the ClamAV daemon (clamd) to be
     * established.
     *
     * @return maximum time in milliseconds to wait for a {@link Socket} connection with the ClamAV daemon (clamd) to be
     * established
     */
    @SuppressWarnings("MagicNumber")
    @AttributeDefinition(
        name = "Connect Timeout (millis)",
        description = "Maximum time in milliseconds to wait for a socket connection with the ClamAV daemon (clamd) "
            + "to be established.",
        defaultValue = "5000",
        type = AttributeType.INTEGER,
        min = "0"
    )
    int clamav_connect$_$timeout() default 5_000;

    /**
     * Maximum time in milliseconds to wait for every single read from the ClamAV daemon (clamd) to complete.
     *
     * @return maximum time in milliseconds to wait for every single read from the ClamAV daemon (clamd) to complete
     */
    @SuppressWarnings("MagicNumber")
    @AttributeDefinition(
        name = "Read Timeout (millis)",
        description = "Maximum time in milliseconds to wait for every single read from the ClamAV daemon (clamd) "
            + "to complete.",
        defaultValue = "60000",
        type = AttributeType.INTEGER,
        min = "0"
    )
    int clamav_read$_$timeout() default 60_000;
}
