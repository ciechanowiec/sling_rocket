package eu.ciechanowiec.sling.rocket.identity;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UnwrappedIteration;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.api.security.user.*;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Session;
import java.security.Principal;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * {@link Authorizable} with simplified API operations on it.
 * </p>
 * <p>
 * The class provides API operations on {@link Authorizable} in a way detached from an ongoing {@link Session}.
 * {@link Session}'s live cycle is supposed to be fully managed by an {@link SimpleAuthorizable}
 * itself in an encapsulated manner.
 * </p>
 */
@Slf4j
@ToString
@SuppressWarnings("WeakerAccess")
public class SimpleAuthorizable {

    private final AuthID authID;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;
    @ToString.Exclude
    private final ImpersonationDefinition impersonationGranter = this::grantImpersonation;
    @ToString.Exclude
    private final ImpersonationDefinition impersonationRevoker = this::revokeImpersonation;

    /**
     * Constructs an instance of this class.
     * @param authID {@link AuthID} of an {@link Authorizable} wrapped by this {@link SimpleAuthorizable}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                       object to acquire access to resources
     */
    public SimpleAuthorizable(AuthID authID, ResourceAccess resourceAccess) {
        this.authID = authID;
        this.resourceAccess = resourceAccess;
    }

    /**
     * All impersonators of this {@link SimpleAuthorizable}.
     * @return all impersonators of this {@link SimpleAuthorizable}
     */
    @SuppressWarnings({"WeakerAccess", "rawtypes", "unchecked"})
    public Collection<AuthIDUser> impersonators() {
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            log.trace("Listing impersonators of '{}'", this);
            return toUser(authID, resourceResolver)
                    .map(SneakyFunction.sneaky(User::getImpersonation))
                    .map(SneakyFunction.sneaky(Impersonation::getImpersonators))
                    .map(
                            principalIterator ->
                                    (UnwrappedIteration<Principal>) new UnwrappedIteration<>(principalIterator)
                    )
                    .map(UnwrappedIteration::list)
                    .orElse(List.of())
                    .stream()
                    .map(SneakyFunction.sneaky(
                            principal -> new WithUserManager(resourceResolver).get().getAuthorizable(principal))
                    )
                    .map(SneakyFunction.sneaky(Authorizable::getID))
                    .map(AuthIDUser::new)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * Grants the right to impersonate this {@link SimpleAuthorizable} to the specified {@link Authorizable}s
     * identified by their {@link AuthID}s. If the impersonation right cannot be granted to any of the specified
     * {@link AuthID}s, those will be ignored.
     * @param impersonatorIDs collection of {@link AuthID}s of {@link Authorizable}s to which the impersonation
     *                        right will be granted
     * @return {@code true} if the impersonation right was successfully granted for all specified {@link AuthID}s;
     *         {@code false} if only some or none of the specified {@link AuthID}s had the impersonation right granted
     */
    @SuppressWarnings("WeakerAccess")
    public boolean grantImpersonation(Collection<AuthIDUser> impersonatorIDs) {
        log.trace(
                "Requested to grant impersonators with these IDs: '{}' impersonations for this impersonable: '{}'",
                impersonatorIDs, this
        );
        boolean wasGranted = defineImpersonation(impersonatorIDs, impersonationGranter);
        log.trace(
                "Were impersonators with these IDs: '{}' granted impersonations for this impersonable: '{}'? "
              + "Answer: {}", impersonatorIDs, this, wasGranted
        );
        return wasGranted;
    }

    /**
     * Grants the right to impersonate this {@link SimpleAuthorizable} to the specified {@link Authorizable}s
     * identified by their {@link AuthID}s. If the impersonation right cannot be granted to any of the specified
     * {@link AuthID}s, those will be ignored.
     * @param impersonatorIDs collection of {@link AuthID}s of {@link Authorizable}s to which the impersonation
     *                        right will be granted
     * @return {@code true} if the impersonation right was successfully granted for all specified {@link AuthID}s;
     *         {@code false} if only some or none of the specified {@link AuthID}s had the impersonation right granted
     */
    @SuppressWarnings("WeakerAccess")
    public boolean grantImpersonation(AuthIDUser... impersonatorIDs) {
        return grantImpersonation(List.of(impersonatorIDs));
    }

    @SneakyThrows
    private boolean grantImpersonation(Principal impersonatorPrincipal, Impersonation impersonableImpersonation) {
        return impersonableImpersonation.grantImpersonation(impersonatorPrincipal);
    }

    /**
     * Revokes the right to impersonate this {@link SimpleAuthorizable} from the specified {@link Authorizable}s
     * identified by their {@link AuthID}s. If the impersonation right cannot be revoked for any of the specified
     * {@link AuthID}s, those will be ignored.
     * @param impersonatorIDs collection of {@link AuthID}s of {@link Authorizable}s from which the impersonation
     *                        right will be revoked
     * @return {@code true} if the impersonation right was successfully revoked for all specified {@link AuthID}s;
     *         {@code false} if only some or none of the specified {@link AuthID}s had the impersonation right revoked
     */
    @SuppressWarnings("WeakerAccess")
    public boolean revokeImpersonation(Collection<AuthIDUser> impersonatorIDs) {
        log.trace(
                "Requested to revoke from impersonators with these IDs: '{}' impersonations "
              + "for this impersonable: '{}'", impersonatorIDs, this
        );
        boolean wasRevoked = defineImpersonation(impersonatorIDs, impersonationRevoker);
        log.trace(
                "Were from impersonators with these IDs: '{}' impersonations for this impersonable: '{}' revoked? "
              + "Answer: {}", impersonatorIDs, this, wasRevoked
        );
        return wasRevoked;
    }

    @SneakyThrows
    private boolean revokeImpersonation(Principal impersonatorPrincipal, Impersonation impersonableImpersonation) {
        return impersonableImpersonation.revokeImpersonation(impersonatorPrincipal);
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private boolean defineImpersonation(
            Collection<AuthIDUser> impersonatorIDs, ImpersonationDefinition impersonationDefinition
    ) {
        log.trace(
                "Requested to define impersonation for impersonators with these IDs: '{}' and this impersonable: '{}'",
                impersonatorIDs, this
        );
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            Collection<Principal> impersonatorsPrincipals = toPrincipals(impersonatorIDs, resourceResolver);
            return toUser(authID, resourceResolver)
                    .map(SneakyFunction.sneaky(User::getImpersonation))
                    .map(SneakyFunction.sneaky(impersonableImpersonation -> {
                        boolean wasDefined = defineImpersonation(
                                impersonatorsPrincipals, impersonableImpersonation, impersonationDefinition
                        );
                        resourceResolver.commit();
                        return wasDefined;
                    }))
                    .orElse(false);
        }
    }

    private boolean defineImpersonation(
            Collection<Principal> impersonatorsPrincipals,
            Impersonation impersonableImpersonation,
            ImpersonationDefinition impersonationDefinition
    ) {
        Collection<Boolean> definitionResults = impersonatorsPrincipals.stream()
                .map(
                        impersonatorPrincipal -> impersonationDefinition.define(
                                impersonatorPrincipal, impersonableImpersonation
                        )
                )
                .collect(Collectors.toUnmodifiableSet());
        return !definitionResults.contains(false);
    }

    /**
     * Add the {@link Authorizable} represented by this {@link SimpleAuthorizable} to a {@link Group}
     * identified by the specified {@link AuthIDGroup}.
     * @param authIDGroup {@link AuthIDGroup} identifying a {@link Group} to which the {@link Authorizable}
     *                    represented by this {@link SimpleAuthorizable} should be added to
     * @return {@code true} if in the end of the adding operation the {@link Authorizable} represented by this
     *         {@link SimpleAuthorizable} is a direct (declared) member of the {@link Group} identified by the
     *         specified {@link AuthIDGroup}; {@code false} otherwise
     */
    public boolean addToGroup(AuthIDGroup authIDGroup) {
        log.trace("Adding '{}' to '{}'", this, authIDGroup);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            toGroup(authIDGroup, resourceResolver).ifPresentOrElse(
                    SneakyConsumer.sneaky(group -> {
                        group.addMembers(authID.get());
                        resourceResolver.commit();
                    }),
                    () -> log.warn("'{}' not found. {} will not be added to that group", authIDGroup, this)
            );
        }
        return groups(true).contains(authIDGroup);
    }

    /**
     * Returns a {@link Collection} of {@link AuthIDGroup} identifying all {@link Group}s to which
     * the {@link Authorizable} represented by this {@link SimpleAuthorizable} belongs to.
     * @param onlyDeclared {@code true} to consider only direct {@link Group} membership;
     *                     {@code false} to consider both direct and indirect
     * @return {@link Collection} of {@link AuthIDGroup} identifying all {@link Group}s to which
     *         the {@link Authorizable} represented by this {@link SimpleAuthorizable} belongs to
     */
    public Collection<AuthIDGroup> groups(boolean onlyDeclared) {
        log.trace("Extracting Groups of {}. Only declared: {}", this, onlyDeclared);
        Function<Authorizable, Iterator<Group>> onlyDeclaredExtractor = SneakyFunction.sneaky(
                Authorizable::declaredMemberOf
        );
        Function<Authorizable, Iterator<Group>> allExtractor = SneakyFunction.sneaky(Authorizable::memberOf);
        return groups(onlyDeclared ? onlyDeclaredExtractor : allExtractor);
    }

    @SneakyThrows
    private Collection<AuthIDGroup> groups(Function<Authorizable, Iterator<Group>> groupsExtractor) {
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            UserManager userManager = new WithUserManager(resourceResolver).get();
            return Optional.ofNullable(userManager.getAuthorizable(authID.get()))
                    .map(groupsExtractor)
                    .map(UnwrappedIteration::new)
                    .map(UnwrappedIteration::list)
                    .orElseGet(
                            () -> {
                                log.warn("Unable to extract Groups of '{}'", this);
                                return List.of();
                            }
                    )
                    .stream()
                    .map(SneakyFunction.sneaky(Authorizable::getID))
                    .map(AuthIDGroup::new)
                    .toList();
        }
    }

    private Collection<Principal> toPrincipals(
            Collection<AuthIDUser> authorizableIDs, ResourceResolver resourceResolver
    ) {
        UserManager userManager = new WithUserManager(resourceResolver).get();
        return authorizableIDs.stream()
                .map(AuthID::get)
                .map(SneakyFunction.sneaky(
                                authorizableID -> Optional.ofNullable(userManager.getAuthorizable(authorizableID))
                        )
                )
                .flatMap(Optional::stream)
                .map(SneakyFunction.sneaky(Authorizable::getPrincipal))
                .toList();
    }

    @SuppressWarnings({"squid:S1905", "unchecked"})
    private Optional<User> toUser(Authorizable authorizable) {
        log.trace("Converting {} to a User", authorizable);
        return (Optional<User>) Conditional.conditional(authorizable.isGroup())
                .onTrue(() -> {
                    log.debug("{} is not a User and will not be converted to User", authorizable);
                    return Optional.empty();
                })
                .onFalse(() -> Optional.of((User) authorizable))
                .get(Optional.class);
    }

    @SneakyThrows
    private Optional<User> toUser(AuthID userID, ResourceResolver resourceResolver) {
        log.trace("Converting {} to a User", userID);
        UserManager userManager = new WithUserManager(resourceResolver).get();
        return Optional.ofNullable(userManager.getAuthorizable(userID.get()))
                .flatMap(this::toUser)
                .or(() -> {
                    log.debug(
                            "An Authorizable with ID '{}' wasn't found when trying to convert it to User", userID
                    );
                    return Optional.empty();
                });
    }

    @SuppressWarnings({"squid:S1905", "unchecked"})
    private Optional<Group> toGroup(Authorizable authorizable) {
        log.trace("Converting {} to a Group", authorizable);
        return (Optional<Group>) Conditional.conditional(authorizable.isGroup())
                .onFalse(() -> {
                    log.debug("{} is not a Group and will not be converted to Group", authorizable);
                    return Optional.empty();
                })
                .onTrue(() -> Optional.of((Group) authorizable))
                .get(Optional.class);
    }

    @SneakyThrows
    private Optional<Group> toGroup(
            @SuppressWarnings("TypeMayBeWeakened") AuthIDGroup authIDGroup, ResourceResolver resourceResolver
    ) {
        log.trace("Converting {} to a Group", authIDGroup);
        UserManager userManager = new WithUserManager(resourceResolver).get();
        return Optional.ofNullable(userManager.getAuthorizable(authIDGroup.get()))
                .flatMap(this::toGroup)
                .or(() -> {
                    log.debug(
                            "An Authorizable with ID '{}' wasn't found when trying to convert it to Group", authIDGroup
                    );
                    return Optional.empty();
                });
    }
}
