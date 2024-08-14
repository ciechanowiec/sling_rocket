package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.ImplementationPicker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Picks appropriate Sling Models for {@link Asset}-s.
 */
@Component(
        service = {AssetImplementationPicker.class, ImplementationPicker.class},
        immediate = true
)
@ServiceDescription("Picks appropriate Sling Models for assets")
@Slf4j
@ToString
@SuppressWarnings("PMD.LongVariable")
public class AssetImplementationPicker implements ImplementationPicker {

    private final Map<String, Class<? extends Asset>> nodeTypesToModelsMapping;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                        object to acquire access to resources
     */
    @Activate
    public AssetImplementationPicker(
            @Reference(cardinality = ReferenceCardinality.MANDATORY)
            ResourceAccess resourceAccess
    ) {
        this.resourceAccess = resourceAccess;
        this.nodeTypesToModelsMapping = Map.of(
                Asset.NT_ASSET_REAL, AssetRealModel.class,
                Asset.NT_ASSET_LINK, AssetLinkModel.class,
                JcrConstants.NT_FILE, NTFileModel.class,
                JcrConstants.NT_RESOURCE, NTResourceModel.class
        );
        log.info("Initialized {}", this);
    }

    @Override
    @Nullable
    public Class<?> pick(
            @NotNull Class<?> adapterType, @NotNull Class<?>[] implementationsTypes, @NotNull Object adaptable
    ) {
        Collection<Class<?>> implementationTypesCollection = List.of(implementationsTypes);
        log.trace(
                "Picking implementation. Adapter type: {}. Implementation types: {}. Adaptable: {}",
                adapterType, implementationTypesCollection, adaptable
        );
        return pick(adapterType, implementationTypesCollection, adaptable);
    }

    @Nullable
    private Class<?> pick(Class<?> adapterType, Collection<Class<?>> implementationTypes, Object adaptable) {
        if (adapterType == Asset.class) {
            log.trace(
                    "Adapter type is of type {}. Picking adapter implementation for {} among {}",
                    Asset.class, adaptable, implementationTypes
            );
            return pick(implementationTypes, adaptable);
        } else {
            log.trace(
                    "Adapter type isn't of type {}. Aborting adapter implementation picking for {} among {}",
                    Asset.class, adaptable, implementationTypes
            );
            return null;
        }
    }

    @Nullable
    private Class<?> pick(Collection<Class<?>> implementationTypes, Object adaptable) {
        if (adaptable instanceof Resource resourceBeingAdapted) {
            log.trace(
                    "{} is of type {}. Picking adapter implementation for it among {}",
                    adaptable, Resource.class, implementationTypes
            );
            return pick(implementationTypes, resourceBeingAdapted);
        } else {
            log.trace("Adaptable isn't of type {}. Aborting adapter implementation picking", Resource.class);
            return null;
        }
    }

    private Class<?> pick(Collection<Class<?>> implementationTypes, Resource resourceBeingAdapted) {
        log.trace("Resource being adapted: {}. Implementation types: {}", resourceBeingAdapted, implementationTypes);
        String pathToResourceBeingAdapted = resourceBeingAdapted.getPath();
        NodeProperties nodeProperties = new NodeProperties(
                new TargetJCRPath(pathToResourceBeingAdapted), resourceAccess
        );
        return pick(implementationTypes, nodeProperties);
    }

    private Class<?> pick(Collection<Class<?>> implementationTypes, NodeProperties nodeProperties) {
        log.trace(
                "Node properties of the resource being adapted: {}. Implementation types: {}",
                nodeProperties, implementationTypes
        );
        Map<String, Class<? extends Asset>> filteredNodeTypesToModelsMapping = nodeTypesToModelsMapping.entrySet()
                .stream()
                .filter(
                        entry -> {
                            Class<? extends Asset> possibleModel = entry.getValue();
                            return implementationTypes.contains(possibleModel);
                        }
                ).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        return pick(filteredNodeTypesToModelsMapping, nodeProperties);
    }

    @Nullable
    private Class<?> pick(
            Map<String, Class<? extends Asset>> filteredNodeTypesToModelsMapping, NodeProperties nodeProperties
    ) {
        log.trace(
                "Filtered node types to models mapping: {}. Node properties of the resource being adapted: {}",
                filteredNodeTypesToModelsMapping, nodeProperties
        );
        String primaryType = nodeProperties.primaryType();
        Class<? extends Asset> pickedModel = filteredNodeTypesToModelsMapping.get(primaryType);
        log.trace("For {} this model was picked: {}", nodeProperties, pickedModel);
        return pickedModel;
    }
}
