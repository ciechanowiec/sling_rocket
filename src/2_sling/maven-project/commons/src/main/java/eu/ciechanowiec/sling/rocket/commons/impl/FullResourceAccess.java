package eu.ciechanowiec.sling.rocket.commons.impl;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.Collections;
import java.util.Map;

/**
 * Provides full and unlimited access to Apache Sling resources, including the underlying Java Content Repository.
 */
@Component(
        service = ResourceAccess.class,
        immediate = true
)
@Slf4j
@ServiceDescription("Provides full and unlimited access to Apache Sling resources, "
                  + "including the underlying Java Content Repository")
public class FullResourceAccess implements ResourceAccess {

    static final String SUBSERVICE_NAME = "sling-rocket-subservice";

    private final ResourceResolverFactory resourceResolverFactory;

    /**
     * Constructs an instance of this class. Supposed to be used exclusively by the OSGi framework
     * during service construction and never directly by clients.
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
     * {@inheritDoc}
     */
    @Override
    @SneakyThrows
    public ResourceResolver acquireAccess() {
        log.trace("Resource resolver requested");
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME
        );
        return resourceResolverFactory.getServiceResourceResolver(authInfo);
    }
}
