package eu.ciechanowiec.sling.rocket.jcr;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.jcr.Node;
import javax.jcr.Property;
import java.util.Optional;

@Slf4j
record ConditionalProperty(String propertyName) {

    @SneakyThrows
    Optional<Property> retrieveFrom(Node node) {
        log.trace("Retrieving property of name '{}' from {}", propertyName, node);
        boolean hasProperty = node.hasProperty(propertyName);
        if (hasProperty) {
            Property property = node.getProperty(propertyName);
            log.trace("Property found and will be returned: {}. Node: {}", property, node);
            return Optional.of(property);
        } else {
            log.trace("No property of name '{}' found for {}", propertyName, node);
            return Optional.empty();
        }
    }
}
