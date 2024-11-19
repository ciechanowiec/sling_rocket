package eu.ciechanowiec.sling.rocket.identity;

import org.apache.jackrabbit.api.security.user.Impersonation;

import java.security.Principal;

@FunctionalInterface
interface ImpersonationDefinition {

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean define(Principal impersonatorPrincipal, Impersonation impersonableImpersonation);
}
