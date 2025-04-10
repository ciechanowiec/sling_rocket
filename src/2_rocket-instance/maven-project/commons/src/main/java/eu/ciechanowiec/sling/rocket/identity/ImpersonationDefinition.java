package eu.ciechanowiec.sling.rocket.identity;

import java.security.Principal;
import org.apache.jackrabbit.api.security.user.Impersonation;

@FunctionalInterface
interface ImpersonationDefinition {

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean define(Principal impersonatorPrincipal, Impersonation impersonableImpersonation);
}
