package eu.ciechanowiec.sling.rocket.observation.audit.pushers;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link GenericRCL}.
 */
@ObjectClassDefinition
public @interface GenericRCLConfig {

    /**
     * Returns the value of the {@link ResourceChangeListener#PATHS} OSGi {@link Component#property()} to be used by the
     * {@link GenericRCL} for catching {@link ResourceChange}s on the specified paths.
     *
     * @return value of the {@link ResourceChangeListener#PATHS} OSGi {@link Component#property()} to be used by the
     * {@link GenericRCL} for catching {@link ResourceChange}s on the specified paths
     */
    @AttributeDefinition(
        name = "Resource Paths",
        description = "Value of the 'resource.paths' OSGi property to be used by the GenericRCL for catching "
            + "resource changes on the specified paths",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String[] resource_paths();

    /**
     * Returns the value of the {@link ResourceChangeListener#CHANGES} OSGi {@link Component#property()} to be used by
     * the {@link GenericRCL} for catching {@link ResourceChange}s on the specified change types.
     *
     * @return value of the {@link ResourceChangeListener#CHANGES} OSGi {@link Component#property()} to be used by the
     * {@link GenericRCL} for catching {@link ResourceChange}s on the specified change types
     */
    @AttributeDefinition(
        name = "Resource Change Types",
        description = "Value of the 'resource.change.types' OSGi property to be used by the GenericRCL for catching "
            + "resource changes on the specified change types",
        type = AttributeType.STRING
    )
    @SuppressWarnings("squid:S100")
    String[] resource_change_types();
}
