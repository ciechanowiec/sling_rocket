package eu.ciechanowiec.sling.rocket.observation.broadcast;

import org.apache.jackrabbit.oak.api.jmx.Description;

import java.util.Optional;

/**
 * MBean for a {@link Broadcast}.
 */
@FunctionalInterface
@SuppressWarnings("WeakerAccess")
@Description(Broadcast.SERVICE_DESCRIPTION)
public interface BroadcastMBean {

    /**
     * Broadcast application statistics.
     *
     * @return {@link Optional} containing the JSON string that represents application statistics that were submitted to
     * be broadcasted; empty {@link Optional} is returned if the application statistics were not sucessfully submitted
     * for broadcast
     */
    @SuppressWarnings("unused")
    @Description("Broadcast application statistics")
    Optional<String> broadcast();
}
