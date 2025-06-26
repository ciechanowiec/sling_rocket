package eu.ciechanowiec.sling.rocket.google;

import com.google.api.services.directory.Directory;
import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.Member;
import com.google.api.services.directory.model.User;
import org.apache.jackrabbit.oak.api.jmx.Description;
import org.apache.jackrabbit.oak.api.jmx.Name;

import java.util.List;
import java.util.Optional;

/**
 * MBean for a {@link GoogleDirectory}.
 */
@Description(GoogleDirectory.SERVICE_DESCRIPTION)
@SuppressWarnings("PMD.LooseCoupling")
public interface GoogleDirectoryMBean {

    /**
     * List all {@link User}s in the {@link Directory}.
     *
     * @return all {@link User}s in the {@link Directory}
     */
    @Description("List all Users in the Directory")
    List<User> listUsers();

    /**
     * List all {@link Group}s in the {@link Directory}.
     *
     * @return all {@link Group}s in the {@link Directory}
     */
    @Description("List all Groups in the Directory")
    List<Group> listGroups();

    /**
     * List all {@link Group}s that a {@link Member} identified by the specified key directly belongs to in the
     * {@link Directory}. The key is the parameter for {@link Directory.Groups.List#setKey(String)}, i.e.:
     * <ol>
     *     <li>For the {@link User} the key is the {@link User}'s primary email address, alias email address,
     *     or unique {@link User}'s ID.</li>
     *     <li>For the {@link Group} the key is the {@link Group}'s email address, {@link Group}'s alias, or the unique
     *     {@link Group}'s ID.</li>
     * </ol>
     * If no {@link Group}s are found or if there is an error listing the {@link Group}s,
     * an empty {@link List} is returned.
     *
     * @param memberKey the key of the {@link Member} whose {@link Group}s must be listed
     * @return all {@link Group}s that a {@link Member} identified by the specified key directly belongs to in the
     * {@link Directory}; an empty {@link List} is returned if none found or on error
     */
    @Description(
        "List all Groups that a Member identified by the specified key directly belongs to in the Directory"
    )
    List<Group> listGroups(
        @Name("memberKey")
        String memberKey
    );

    /**
     * Retrieve the {@link User} by their key. The key is the parameter for {@link Directory.Users#get(String)}, i.e.
     * {@link User}'s primary email address, alias email address, or unique {@link User}'s ID. If no {@link User} is
     * found for the given key, an empty {@link Optional} is returned.
     *
     * @param userKey the key of the {@link User} to retrieve
     * @return an Optional containing the {@link User} if found, or empty if not found
     */
    @Description("Retrieve the User by their key")
    Optional<User> retrieveUser(
        @Name("userKey")
        String userKey
    );

    /**
     * Retrieve the {@link Group} by its key. The key is the parameter for {@link Directory.Groups#get(String)}, i.e.
     * {@link Group}'s email address, {@link Group}'s alias, or the unique {@link Group}s ID. If no {@link Group} is
     * found for the given key, an empty {@link Optional} is returned.
     *
     * @param groupKey the key of the {@link Group} to retrieve
     * @return an Optional containing the {@link Group} if found, or empty if not found
     */
    @Description("Retrieve the Group by its key")
    Optional<Group> retrieveGroup(
        @Name("groupKey")
        String groupKey
    );

    /**
     * List all direct {@link Member}s of the {@link Group} identified by the specified key. The key is the parameter
     * for {@link Directory.Members#list(String)}, i.e. {@link Group}'s email address, {@link Group}'s alias, or the
     * unique {@link Group}'s ID. If no {@link Member}s are found or if there is an error retrieving the
     * {@link Member}s, an empty {@link List} is returned.
     *
     * @param groupKey the key of the {@link Group} whose {@link Member}s must be listed
     * @return all direct {@link Member}s of the {@link Group} identified by the specified key, or an empty {@link List}
     * if none found or on error
     */
    @Description("List all direct Members of the Group identified by the specified key")
    List<Member> listMembers(
        @Name("groupKey")
        String groupKey
    );
}
