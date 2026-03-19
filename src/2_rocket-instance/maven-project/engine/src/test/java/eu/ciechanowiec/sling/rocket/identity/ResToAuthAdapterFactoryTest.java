package eu.ciechanowiec.sling.rocket.identity;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResToAuthAdapterFactoryTest extends TestEnvironment {

    ResToAuthAdapterFactoryTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @SneakyThrows
    @Test
    @SuppressWarnings("PMD.CloseResource")
    void testAdaptation() {
        ResourceResolver resourceResolver = context.resourceResolver();
        AuthIDUser user = createOrGetUser(new AuthIDUser("user-1"));
        AuthIDGroup group = createOrGetGroup(new AuthIDGroup("group-1"));
        UserManager userManager = new WithUserManager(resourceResolver).get();
        Authorizable userAuth = Objects.requireNonNull(userManager.getAuthorizable(user.get()));
        Authorizable groupAuth = Objects.requireNonNull(userManager.getAuthorizable(group.get()));
        String userPath = userAuth.getPath();
        String groupPath = groupAuth.getPath();
        Authorizable adaptedUserAuth = Optional.ofNullable(resourceResolver.getResource(userPath))
            .map(resource -> resource.adaptTo(Authorizable.class))
            .orElseThrow();
        Authorizable adaptedGroupAuth = Optional.ofNullable(resourceResolver.getResource(groupPath))
            .map(resource -> resource.adaptTo(Authorizable.class))
            .orElseThrow();
        User adaptedUser = Optional.ofNullable(resourceResolver.getResource(userPath))
            .map(resource -> resource.adaptTo(User.class))
            .orElseThrow();
        Group adaptedGroup = Optional.ofNullable(resourceResolver.getResource(groupPath))
            .map(resource -> resource.adaptTo(Group.class))
            .orElseThrow();
        Optional<Group> emptyAdaptationGroup = Optional.ofNullable(resourceResolver.getResource(userPath))
            .map(resource -> resource.adaptTo(Group.class));
        Optional<User> emptyAdaptationUser = Optional.ofNullable(resourceResolver.getResource(groupPath))
            .map(resource -> resource.adaptTo(User.class));
        assertAll(
            () -> assertEquals(userAuth.getID(), adaptedUserAuth.getID()),
            () -> assertEquals(groupAuth.getID(), adaptedGroupAuth.getID()),
            () -> assertEquals(userAuth.getID(), adaptedUser.getID()),
            () -> assertEquals(groupAuth.getID(), adaptedGroup.getID()),
            () -> assertTrue(emptyAdaptationGroup.isEmpty()),
            () -> assertTrue(emptyAdaptationUser.isEmpty())
        );
    }
}
