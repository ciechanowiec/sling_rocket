package eu.ciechanowiec.sling.rocket.identity.mapping;

import eu.ciechanowiec.sling.rocket.identity.*;
import eu.ciechanowiec.sling.rocket.identity.creation.AuthCreationBroadcast;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Slf4j
@SuppressWarnings(
    {
        "MethodLength", "JavaNCSS", "VariableDeclarationUsageDistance", "MultipleStringLiterals",
        "PMD.UnitTestShouldIncludeAssert", "PMD.AvoidDuplicateLiterals"
    }
)
class GroupToAuthMappingTest extends TestEnvironment {

    GroupToAuthMappingTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        context.registerInjectActivateService(
            AuthCreationBroadcast.class, Map.of(
                ResourceChangeListener.PATHS,
                new String[]{"/"},
                ResourceChangeListener.CHANGES,
                new String[]{ResourceChangeListener.CHANGE_ADDED}
            )
        );
    }

    @SneakyThrows
    @Test
    void basicTest() {
        context.registerInjectActivateService(
            GroupToAuthMapping.class, Map.of(
                "groups-to-auths.mappings",
                new String[]{
                    "group1###user1",
                    "group2###user1<<<>>>user4",
                    "group4###user3<<<>>>user5<<<>>>group5"
                }
            )
        );
        SimpleAuthorizable user1 = new SimpleAuthorizable(new AuthIDUser("user1"), fullResourceAccess);
        SimpleAuthorizable user2 = new SimpleAuthorizable(new AuthIDUser("user2"), fullResourceAccess);
        SimpleAuthorizable user3 = new SimpleAuthorizable(new AuthIDUser("user3"), fullResourceAccess);
        SimpleAuthorizable user4 = new SimpleAuthorizable(new AuthIDUser("user4"), fullResourceAccess);
        SimpleAuthorizable user5 = new SimpleAuthorizable(new AuthIDUser("user5"), fullResourceAccess);
        SimpleAuthorizable user6 = new SimpleAuthorizable(new AuthIDUser("user6"), fullResourceAccess);
        SimpleAuthorizable group1 = new SimpleAuthorizable(new AuthIDGroup("group1"), fullResourceAccess);
        SimpleAuthorizable group2 = new SimpleAuthorizable(new AuthIDGroup("group2"), fullResourceAccess);
        SimpleAuthorizable group3 = new SimpleAuthorizable(new AuthIDGroup("group3"), fullResourceAccess);
        SimpleAuthorizable group4 = new SimpleAuthorizable(new AuthIDGroup("group4"), fullResourceAccess);
        SimpleAuthorizable group5 = new SimpleAuthorizable(new AuthIDGroup("group5"), fullResourceAccess);
        createOrGetGroup(new AuthIDGroup(group1.authID().get()));
        createOrGetGroup(new AuthIDGroup(group2.authID().get()));
        createOrGetGroup(new AuthIDGroup(group3.authID().get()));
        createOrGetGroup(new AuthIDGroup(group4.authID().get()));
        createOrGetGroup(new AuthIDGroup(group5.authID().get()));
        createOrGetUser(new AuthIDUser(user1.authID().get()));
        createOrGetUser(new AuthIDUser(user2.authID().get()));
        createOrGetUser(new AuthIDUser(user3.authID().get()));
        createOrGetUser(new AuthIDUser(user4.authID().get()));
        createOrGetUser(new AuthIDUser(user5.authID().get()));
        createOrGetUser(new AuthIDUser(user6.authID().get()));
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(
            () -> {
                assertEquals(
                    Set.of(
                        new AuthIDGroup("group1"), new AuthIDGroup("group2")
                    ), user1.groups(true)
                );
                assertEquals(Set.of(), user2.groups(true));
                assertEquals(
                    Set.of(
                        new AuthIDGroup("group4")
                    ), user3.groups(true)
                );
                assertEquals(
                    Set.of(
                        new AuthIDGroup("group2")
                    ), user4.groups(true)
                );
                assertEquals(
                    Set.of(
                        new AuthIDGroup("group2")
                    ), user4.groups(true)
                );
                assertEquals(
                    Set.of(
                        new AuthIDGroup("group4")
                    ), user5.groups(true)
                );
                assertEquals(Set.of(), user6.groups(true));
                assertEquals(
                    Set.of(
                        new AuthIDGroup("group4")
                    ), group5.groups(true)
                );
                assertEquals(1, numOfDeclaredMembers(new AuthIDGroup(group1.authID().get())));
                assertEquals(2, numOfDeclaredMembers(new AuthIDGroup(group2.authID().get())));
                assertEquals(0, numOfDeclaredMembers(new AuthIDGroup(group3.authID().get())));
                assertEquals(3, numOfDeclaredMembers(new AuthIDGroup(group4.authID().get())));
                assertEquals(0, numOfDeclaredMembers(new AuthIDGroup(group5.authID().get())));
            }
        );
    }

    @SuppressWarnings("TypeMayBeWeakened")
    @SneakyThrows
    private int numOfDeclaredMembers(AuthIDGroup authIDGroup) {
        log.debug("Getting number of declared members of {}", authIDGroup);
        UserManager userManager = new WithUserManager(fullResourceAccess.acquireAccess()).get();
        Group group = Optional.ofNullable(userManager.getAuthorizable(authIDGroup.get()))
            .filter(Authorizable::isGroup)
            .map(Group.class::cast)
            .orElseThrow();
        return (int) IteratorUtils.stream(group.getDeclaredMembers()).count();
    }

    @Test
    @SneakyThrows
    void modificationTest() {
        // Activate with initial config
        GroupToAuthMapping groupToAuthMapping = context.registerInjectActivateService(
            GroupToAuthMapping.class, Map.of(
                "groups-to-auths.mappings",
                new String[]{
                    "group1###user1",
                    "group2###user2"
                }
            )
        );
        // Create authorizables
        createOrGetGroup(new AuthIDGroup("group1"));
        createOrGetGroup(new AuthIDGroup("group2"));
        createOrGetUser(new AuthIDUser("user1"));
        createOrGetUser(new AuthIDUser("user2"));

        SimpleAuthorizable user1 = new SimpleAuthorizable(new AuthIDUser("user1"), fullResourceAccess);
        SimpleAuthorizable user2 = new SimpleAuthorizable(new AuthIDUser("user2"), fullResourceAccess);

        // Await initial mapping
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(
            () -> {
                assertEquals(Set.of(new AuthIDGroup("group1")), user1.groups(true));
                assertEquals(Set.of(new AuthIDGroup("group2")), user2.groups(true));
            }
        );

        groupToAuthMapping.configure(
            new GroupToAuthMappingConfig() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return GroupToAuthMappingConfig.class;
                }

                @Override
                public String[] groups$_$to$_$auths_mappings() {
                    return new String[]{
                        "group1###user2", // user2 now in group1
                        "group2###user1"  // user1 now in group2
                    };
                }

                @Override
                public String schedule$_$cycle_cron$_$expression() {
                    return "";
                }
            }
        );

        // Mapping must be manually triggered
        Map<AuthID, Set<AuthIDGroup>> mappedAuths = groupToAuthMapping.mapAll();

        assertAll(
            () -> assertEquals(Set.of(new AuthIDGroup("group2")), mappedAuths.get(new AuthIDUser("user1"))),
            () -> assertEquals(Set.of(new AuthIDGroup("group1")), mappedAuths.get(new AuthIDUser("user2")))
        );
    }

    @Test
    @SneakyThrows
    void mapAllCorrectsMembershipsTest() {
        GroupToAuthMapping groupToAuthMapping = context.registerInjectActivateService(
            GroupToAuthMapping.class, Map.of(
                "groups-to-auths.mappings",
                new String[]{
                    "group1###user1",
                    "group2###user2"
                }
            )
        );

        // Create authorizables
        createOrGetGroup(new AuthIDGroup("group1"));
        createOrGetGroup(new AuthIDGroup("group2"));
        createOrGetUser(new AuthIDUser("user1"));
        createOrGetUser(new AuthIDUser("user2"));
        createOrGetUser(new AuthIDUser("user3"));

        SimpleAuthorizable user1 = new SimpleAuthorizable(new AuthIDUser("user1"), fullResourceAccess);
        SimpleAuthorizable user2 = new SimpleAuthorizable(new AuthIDUser("user2"), fullResourceAccess);
        SimpleAuthorizable user3 = new SimpleAuthorizable(new AuthIDUser("user3"), fullResourceAccess);
        AuthIDGroup group1Id = new AuthIDGroup("group1");
        AuthIDGroup group2Id = new AuthIDGroup("group2");

        // Await initial correct mapping
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(
            () -> {
                assertEquals(Set.of(group1Id), user1.groups(true));
                assertEquals(Set.of(group2Id), user2.groups(true));
                assertTrue(user3.groups(true).isEmpty());
            }
        );

        // Manually mess up memberships
        user1.removeFromGroup(group1Id);
        user1.addToGroup(group2Id);
        user3.addToGroup(group1Id);

        assertAll(
            () -> assertEquals(Set.of(group2Id), user1.groups(true)),
            () -> assertEquals(Set.of(group2Id), user2.groups(true)),
            () -> assertEquals(Set.of(group1Id), user3.groups(true))
        );

        // Fix with mapAll
        groupToAuthMapping.mapAll();

        assertAll(
            () -> assertEquals(Set.of(group1Id), user1.groups(true)),
            () -> assertEquals(Set.of(group2Id), user2.groups(true)),
            () -> assertTrue(user3.groups(true).isEmpty())
        );
    }

    @Test
    @SneakyThrows
    void invalidAndEmptyMappingsTest() {
        GroupToAuthMapping groupToAuthMapping = context.registerInjectActivateService(
            GroupToAuthMapping.class, Map.of(
                "groups-to-auths.mappings",
                new String[]{
                    "group1", // invalid
                    "group2###", // invalid value (will be filtered)
                    "###user1", // invalid key
                    "group4###user4<<<>>> <<<>>>user5" // some blank values in a list
                }
            )
        );

        createOrGetUser(new AuthIDUser("user1"));
        createOrGetUser(new AuthIDUser("user4"));
        createOrGetUser(new AuthIDUser("user5"));
        createOrGetGroup(new AuthIDGroup("group1"));
        createOrGetGroup(new AuthIDGroup("group2"));
        createOrGetGroup(new AuthIDGroup("group4"));

        SimpleAuthorizable user1 = new SimpleAuthorizable(new AuthIDUser("user1"), fullResourceAccess);
        SimpleAuthorizable user4 = new SimpleAuthorizable(new AuthIDUser("user4"), fullResourceAccess);
        SimpleAuthorizable user5 = new SimpleAuthorizable(new AuthIDUser("user5"), fullResourceAccess);
        AuthIDGroup group4Id = new AuthIDGroup("group4");

        groupToAuthMapping.mapAll();

        // Await and verify only valid mappings are applied
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(
            () -> {
                assertTrue(user1.groups(true).isEmpty());
                assertEquals(Set.of(group4Id), user4.groups(true));
                assertEquals(Set.of(group4Id), user5.groups(true));
            }
        );

        // Reconfigure to empty the group using a fake user, as "group4###" would be ignored
        groupToAuthMapping.configure(
            new GroupToAuthMappingConfig() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return GroupToAuthMappingConfig.class;
                }

                @Override
                public String[] groups$_$to$_$auths_mappings() {
                    return new String[]{
                        "group4###dummyUser"
                    };
                }

                @Override
                public String schedule$_$cycle_cron$_$expression() {
                    return "";
                }
            }
        );
        createOrGetUser(new AuthIDUser("dummyUser"));
        groupToAuthMapping.process(mock(Job.class));

        assertAll(
            () -> assertTrue(user4.groups(true).isEmpty()),
            () -> assertTrue(user5.groups(true).isEmpty())
        );
    }
}
