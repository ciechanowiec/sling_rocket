package eu.ciechanowiec.sling.rocket.commons;

import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.identity.SimpleAuthorizable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.jcr.Repository;
import java.util.Collections;
import java.util.Map;

/**
 * Provides full and unlimited access to Apache Sling resources, including the underlying {@link Repository}.
 */
@Component(
        service = {ResourceAccess.class, FullResourceAccess.class},
        immediate = true
)
@Slf4j
@ServiceDescription(
        "Provides full and unlimited access to Apache Sling resources, including the underlying Repository"
)
public class FullResourceAccess implements ResourceAccess {

    /**
     * Value of {@link ServiceUserMapped#SUBSERVICENAME}.
     */
    public static final String SUBSERVICE_NAME = "sling-rocket-subservice";
    private static final String SUBSERVICE_USER_ID = "sling-rocket-admin";

    private final ResourceResolverFactory resourceResolverFactory;

    /**
     * Constructs an instance of this class.
     * @param serviceUserMapped Apache Sling service user mapping used to provide access to the resources
     * @param resourceResolverFactory factory used to provide access to the resources
     */
    @Activate
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public FullResourceAccess(
            @Reference(
                    cardinality = ReferenceCardinality.MANDATORY,
                    target = "(" + ServiceUserMapped.SUBSERVICENAME + "=" + SUBSERVICE_NAME + ")"
            )
            ServiceUserMapped serviceUserMapped, // used only to enforce sub-service binding
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            ResourceResolverFactory resourceResolverFactory
    ) {
        this.resourceResolverFactory = resourceResolverFactory;
    }

    /**
     * Provides full and unlimited access to Apache Sling resources, including the underlying {@link Repository}.
     * @return {@link ResourceResolver} that provides the resource access
     */
    @Override
    @SneakyThrows
    public ResourceResolver acquireAccess() {
        log.trace("Resource Resolver requested");
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME
        );
        return resourceResolverFactory.getServiceResourceResolver(authInfo);
    }

    /**
     * Returns a {@link ResourceResolver} that provides access to Apache Sling resources via
     * impersonating the {@link User} with the specified {@link AuthIDUser}. The scope of the access provided
     * by the returned {@link ResourceResolver} is equal to the scope of the access configured for the {@link User}.
     * @param authIDUser {@link AuthIDUser} representing the {@link User} to be impersonated
     * @return {@link ResourceResolver} that provides access to Apache Sling resources
     *         via impersonating the {@link User} with the specified {@link AuthIDUser}
     */
    @SneakyThrows
    public ResourceResolver acquireAccess(AuthIDUser authIDUser) {
        log.trace("Resource Resolver for {} requested", authIDUser);
        SimpleAuthorizable impersonatedUser = new SimpleAuthorizable(authIDUser, this);
        AuthIDUser subserviceAuthID = new AuthIDUser(SUBSERVICE_USER_ID);
        boolean wasGranted = impersonatedUser.grantImpersonation(subserviceAuthID);
        log.trace("Was {} granted the right to impersonate {}? Answer: {}", subserviceAuthID, authIDUser, wasGranted);
        Map<String, Object> authInfo = Map.of(
                ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME,
                ResourceResolverFactory.USER_IMPERSONATION, authIDUser.get()
        );
        log.trace("Auth info: {}", authInfo);
        return resourceResolverFactory.getServiceResourceResolver(authInfo);
    }
}
