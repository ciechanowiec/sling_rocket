package eu.ciechanowiec.sling.rocket.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
class SchedulableRunnableID {

    private final Supplier<String> id;

    SchedulableRunnableID(ServiceReference<?> serviceReference) {
        id = () -> Optional.ofNullable(serviceReference.getProperty(Constants.SERVICE_PID))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .or(
                () -> {
                    String componentName = Optional.ofNullable(
                            serviceReference.getProperty(ComponentConstants.COMPONENT_NAME)
                        ).filter(String.class::isInstance)
                        .map(String.class::cast)
                        .orElse(StringUtils.EMPTY);
                    String componentID = Optional.ofNullable(
                            serviceReference.getProperty(ComponentConstants.COMPONENT_ID)
                        ).filter(Long.class::isInstance)
                        .map(Long.class::cast)
                        .map(String::valueOf)
                        .or(
                            () -> Optional.ofNullable(serviceReference.getProperty(ComponentConstants.COMPONENT_ID))
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                        ).orElse(StringUtils.EMPTY);
                    return Optional.of("%s~%s".formatted(componentName, componentID))
                        .filter(nameAndID -> nameAndID.length() >= 3)
                        .filter(nameAndID -> !nameAndID.endsWith("~"));
                }
            ).orElseThrow(
                () -> new IllegalArgumentException(
                    "Unable to resolve ID for " + serviceReference
                )
            );
    }

    String get() {
        return id.get();
    }

    @Override
    public String toString() {
        return "SchedulableRunnableID{"
            + "id=" + id.get()
            + '}';
    }
}
