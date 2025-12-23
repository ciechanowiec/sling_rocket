package eu.ciechanowiec.sling.rocket.identity;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Repository;
import javax.jcr.query.Query;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository for {@link Authorizable}s.
 */
@Slf4j
public class AuthRepository {

    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     *
     * @param resourceAccess {@link ResourceAccess} that will be used to acquire access to resources
     */
    public AuthRepository(ResourceAccess resourceAccess) {
        this.resourceAccess = resourceAccess;
    }

    /**
     * Returns all {@link Authorizable}s in the {@link Repository}.
     *
     * @return {@link Set} of all {@link Authorizable}s in the repository
     */
    public Set<AuthID> all() {
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String query = "SELECT * FROM [%s]".formatted(UserConstants.NT_REP_AUTHORIZABLE);
            Set<AuthID> all = IteratorUtils.stream(resourceResolver.findResources(query, Query.JCR_SQL2))
                .map(resource -> resource.adaptTo(Authorizable.class))
                .filter(Objects::nonNull)
                .map(SneakyFunction.sneaky(Authorizable::getID))
                .map(AuthIDUniversal::new)
                .map(AuthID.class::cast)
                .collect(Collectors.toUnmodifiableSet());
            int numOfAuths = all.size();
            log.debug("Retrieved {} authorizables", numOfAuths);
            return all;
        }
    }
}
