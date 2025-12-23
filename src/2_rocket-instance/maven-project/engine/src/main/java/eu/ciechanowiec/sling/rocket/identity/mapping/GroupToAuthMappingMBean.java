package eu.ciechanowiec.sling.rocket.identity.mapping;

import eu.ciechanowiec.sling.rocket.identity.AuthID;
import eu.ciechanowiec.sling.rocket.identity.AuthIDGroup;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.oak.api.jmx.Description;
import org.apache.jackrabbit.oak.api.jmx.Name;

import javax.jcr.Repository;
import java.util.Map;
import java.util.Set;

/**
 * MBean for a {@link GroupToAuthMapping}.
 */
@Description(GroupToAuthMapping.SERVICE_DESCRIPTION)
@SuppressWarnings("WeakerAccess")
public interface GroupToAuthMappingMBean {

    /**
     * Performs the {@link #map(AuthID)} operation for all {@link Authorizable}s in the {@link Repository}.
     *
     * @return {@link Map} that aggregates all results of the {@link #map(AuthID)} operation
     */
    @Description("Performs the map operation for all Authorizables in the Repository")
    Map<AuthID, Set<AuthIDGroup>> mapAll();

    /**
     * Ensures that the {@link Authorizable} identified by the provided {@link AuthID} <strong>is</strong> a member of
     * all {@link Group}s to which it should belong according to the configured mappings of this
     * {@link GroupToAuthMapping} service and
     * <strong>is not</strong> a member of any {@link Group} from the configured mappings of this
     * {@link GroupToAuthMapping} service if that {@link Group} does not include the provided {@link AuthID} in its
     * mapping.
     *
     * @param authID {@link AuthID} that identifies the {@link Authorizable} to be mapped
     * @return {@link Map.Entry} where the key is the provided {@link AuthID} and the value is the {@link Set} of
     * {@link AuthIDGroup}s that identify all groups to which the {@link Authorizable} identified by the provided
     * {@link AuthID} belongs after the mapping, both directly and indirectly
     */
    @Description(
        "Ensures that the Authorizable identified by the provided AuthID is a member of all Groups to which it "
            + "should belong according to the configured mappings and is not a member of any Group from the configured "
            + "mappings if that Group does not include the provided AuthID in its mapping"
    )
    Map.Entry<AuthID, Set<AuthIDGroup>> map(
        @Name("authID")
        @Description("AuthID that identifies the Authorizable to be mapped")
        AuthID authID
    );

    /**
     * Returns the currently configured mappings.
     *
     * @return currently configured mappings
     */
    @Description("Returns the currently configured mappings")
    Map<AuthIDGroup, Set<AuthID>> getMappings();
}
