package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static eu.ciechanowiec.sneakyfun.SneakyFunction.sneaky;

/**
 * <p>
 * Represents {@link Property}-ies of a {@link Node}.
 * </p>
 * <p>
 * The class provides API operations on {@link Property}-ies
 * in a way detached from an ongoing {@link Session}. {@link Session}'s live cycle is supposed to be fully
 * managed by {@link NodeProperties} itself in an encapsulated manner.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
@ToString
public class NodeProperties {

    private final JCRPath jcrPath;
    @ToString.Exclude
    private final ResourceAccess resourceAccess;

    /**
     * Constructs an instance of this class.
     * @param jcrNodePath {@link JCRPath} to the underlying {@link Node}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                        object to acquire access to resources
     */
    public NodeProperties(JCRPath jcrNodePath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrNodePath;
        this.resourceAccess = resourceAccess;
        log.trace("Initialized {}", this);
    }

    /**
     * Constructs an instance of this class.
     * @param withJCRPath object that contains a {@link JCRPath} to the underlying {@link Node}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed
     *                        object to acquire access to resources
     */
    public NodeProperties(WithJCRPath withJCRPath, ResourceAccess resourceAccess) {
        this(withJCRPath.jcrPath(), resourceAccess);
    }

    /**
     * Retrieves the primary type of the underlying {@link Node}.
     * @return primary type of the underlying {@link Node}
     */
    public String primaryType() {
        log.trace("Retrieving the primary type of {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                           .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
                           .flatMap(sneaky(node -> Optional.ofNullable(node.getPrimaryNodeType())))
                           .map(NodeType::getName)
                           .orElseThrow();
        }
    }

    /**
     * Checks if the underlying {@link Node} is of the specified primary type.
     * @param expectedPrimaryType name of the primary type against which the check is performed
     * @return {@code true} if the underlying {@link Node} is of the specified primary type; {@code false} otherwise
     */
    public boolean isPrimaryType(String expectedPrimaryType) {
        log.trace("Checking if {} is of this primary type: '{}'", this, expectedPrimaryType);
        String actualPrimaryType = primaryType();
        return actualPrimaryType.equals(expectedPrimaryType);
    }

    /**
     * Asserts that the underlying {@link Node} is of the specified primary type.
     * @param expectedPrimaryType name of the primary type against which the assertion is performed
     * @throws IllegalPrimaryTypeException if the underlying {@link Node} isn't of the specified primary type
     */
    public void assertPrimaryType(String expectedPrimaryType) {
        log.trace("Asserting that {} is of '{}' primary type", this, expectedPrimaryType);
        Conditional.isTrueOrThrow(
                isPrimaryType(expectedPrimaryType), new IllegalPrimaryTypeException(expectedPrimaryType)
        );
    }

    /**
     * <p>
     * Retrieves the {@link Value} of the specified {@link Property} and converts it into the given type.
     * </p>
     *     <ol>
     *         <li>
     * It is guaranteed that the method supports types specified in the {@link Class} fields of
     * {@link DefaultProperties}. Other types and translations between types are supported to the extent that they
     * are supported by the underlying {@link ValueMap#get(String, Object)} method.
     *         </li>
     *         <li>
     * To ensure that the supported type is passed, it is recommended to use factory {@code of} methods from
     * {@link DefaultProperties} when specifying the default value, e.g.:
     *    <pre>{@code
     *    double price = nodeProperties.propertyValue("price", DefaultProperties.of(99.99));
     *    }</pre>
     *         </li>
     *         <li>
     *             <p>
     * Both unary and multi-valued {@link Property}-ies are supported.
     *             </p>
     * Usage example with a <i>unary</i> {@link Property}:
     *    <pre>{@code
     *    long price = nodeProperties.propertyValue("price", DefaultProperties.LONG_ZERO);
     *    String name = nodeProperties.propertyValue("name", DefaultProperties.STRING_EMPTY);
     *    }</pre>
     * Usage example with a <i>multi-valued</i> {@link Property}:
     *    <pre>{@code
     *    long[] prices = nodeProperties.propertyValue("prices", new long[]{DefaultProperties.LONG_ZERO});
     *    String[] names = nodeProperties.propertyValue("names", new String[]{DefaultProperties.STRING_EMPTY});
     *    }</pre>
     *         </li>
     *     </ol>
     * @param propertyName name of the {@link Property} from which the {@link Value} should be retrieved
     * @param defaultValue default {@link Value} to use if the specified {@link Property} does not exist or
     *                     its value cannot be converted to the requested type; the {@code defaultValue} is also used
     *                     to define the type to convert the {@link Value} to
     * @return {@link Value} of the specified {@link Property} converted to the requested type or the
     *         {@code defaultValue} if the specified {@link Property} does not exist or its {@link Value} cannot be
     *         converted to the requested type
     * @param <T> expected type
     */
    public <T> T propertyValue(String propertyName, T defaultValue) {
        log.trace("Getting '{}' property value for {}", propertyName, this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                           .map(Resource::getValueMap)
                           .map(WithPrimitiveArrayTranslation::new)
                           .map(valueMapWithTranslation -> valueMapWithTranslation.get(propertyName, defaultValue))
                           .orElse(defaultValue);
        }
    }

    /**
     * <p>
     * Retrieves the {@link Value} of the specified {@link Property} and converts it into the given type.
     * </p>
     *     <ol>
     *         <li>
     * It is guaranteed that the method supports types specified in the {@link Class} fields of
     * {@link DefaultProperties}. Other types and translations between types are supported to the extent that they
     * are supported by the underlying {@link ValueMap#get(String, Class)} method.
     *         </li>
     *         <li>
     * To ensure that the supported type is passed, it is recommended to use {@link Class} fields of
     * {@link DefaultProperties} when specifying the default value, e.g.:
     *    <pre>{@code
     *    Optional<Double> price = nodeProperties.propertyValue("price", DefaultProperties.DOUBLE_CLASS);
     *    }</pre>
     *         </li>
     *         <li>
     *             <p>
     * Both unary and multi-valued {@link Property}-ies are supported.
     *             </p>
     *             <p>
     * Usage example with a <i>unary</i> {@link Property}:
     *    <pre>{@code
     *    Optional<Long> price = nodeProperties.propertyValue("price", DefaultProperties.LONG_CLASS);
     *    Optional<String> name = nodeProperties.propertyValue("name", DefaultProperties.STRING_CLASS);}
     *    </pre>
     * Usage example with a <i>multi-valued</i> {@link Property}:
     *    <pre>{@code
     *    Optional<long[]> prices = nodeProperties.propertyValue("prices", DefaultProperties.LONG_CLASS_ARRAY);
     *    Optional<String[]> names = nodeProperties.propertyValue("names", DefaultProperties.STRING_CLASS_ARRAY);
     *    }</pre>
     *         </li>
     *     </ol>
     * @param propertyName name of the {@link Property} from which the {@link Value} should be retrieved
     * @param type {@link Class} with the type that should represent the requested {@link Value} and which that
     *             {@link Value} will be cast to
     * @return {@link Optional} containing the {@link Value} of the specified {@link Property} converted to the
     *          requested type; if the specified {@link Property} does not exist or its {@link Value} cannot be
     *          converted to the requested type, an empty {@link Optional} is returned
     * @param <T> expected type
     */
    public <T> Optional<T> propertyValue(String propertyName, Class<T> type) {
        log.trace("Getting '{}' property value for {}", propertyName, this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                    .map(Resource::getValueMap)
                    .map(WithPrimitiveArrayTranslation::new)
                    .flatMap(valueMapWithTranslation -> valueMapWithTranslation.get(propertyName, type));
        }
    }

    /**
     * Checks if the underlying {@link Node} contains a {@link Property} that has the specified name.
     * @param propertyName name of the hypothetically existent {@link Property}
     * @return {@code true} if the underlying {@link Node} contains a {@link Property} that has the specified name;
     *         {@code false} otherwise
     */
    public boolean containsProperty(String propertyName) {
        log.trace("Checking if '{}' contains property of this name: '{}'", this, propertyName);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                           .map(Resource::getValueMap)
                           .map(valueMap -> valueMap.containsKey(propertyName))
                           .orElse(false);
        }
    }

    /**
     * Retrieves the {@link Value} of a {@link Property} of type {@link PropertyType#BINARY} as a {@link File}.
     *
     * @param propertyName name of the {@link Property} from which the {@link Value} of type {@link PropertyType#BINARY}
     *                     should be retrieved
     * @return {@link Value} of a {@link Property} of type {@link PropertyType#BINARY} as a {@link File};
     *         empty {@link Optional} is returned if the {@link Property} isn't of type {@link PropertyType#BINARY}
     *         or doesn't exist
     */
    public Optional<File> retrieveFile(String propertyName) {
        log.trace("Getting the value of the '{}' property as a file. {}", propertyName, this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                           .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
                           .flatMap(node -> new ConditionalProperty(propertyName).retrieveFrom(node))
                           .map(sneaky(Property::getValue))
                           .flatMap(this::asBinary)
                           .map(this::asFile);
        }
    }

    /**
     * Returns all {@link Property}-ies of the underlying {@link Node} as a {@link Map}
     * of {@link Property} names to {@link Property} {@link Value}-s converted to {@link String}; if a
     * given {@link Value} cannot be converted to {@link String}, it is omitted from the result.
     * @return all {@link Property}-ies of the underlying {@link Node} as a {@link Map}
     *         of {@link Property} names to {@link Property} {@link Value}-s converted to {@link String}; if a
     *         given {@link Value} cannot be converted to {@link String}, it is omitted from the result
     */
    public Map<String, String> all() {
        log.trace("Retrieving all properties of {}", this);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                    .map(Resource::getValueMap)
                    .map(ValueMap::keySet)
                    .orElse(Set.of())
                    .stream()
                    .map(propertyName -> Map.entry(
                            propertyName, propertyValue(propertyName, DefaultProperties.STRING_CLASS))
                    )
                    .filter(entry -> entry.getValue().isPresent())
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue().orElseThrow()))
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first)
                    );
        }
    }

    /**
     * Sets the value of the specified {@link Property} according to the logic described in
     * {@link Node#setProperty(String, String)}.
     * @param name name of the {@link Property} to set
     * @param value value of the {@link Property} to set
     * @return {@link Optional} containing this {@link NodeProperties} if the {@link Property} was set successfully;
     *         an empty {@link Optional} is returned if the {@link Property} wasn't set due to any reason
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public Optional<NodeProperties> setProperty(String name, String value) {
        log.trace("Setting property '{}' to '{}'", name, value);
        try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
            String jcrPathRaw = jcrPath.get();
            Optional<NodeProperties> result = Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
                    .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
                    .flatMap(node -> setProperty(node, name, value));
            result.ifPresent(SneakyConsumer.sneaky(nodeProperties -> resourceResolver.commit()));
            return result;
        }
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    private Optional<NodeProperties> setProperty(Node node, String name, String value) {
        try {
            node.setProperty(name, value);
            log.trace("Property '{}' set to '{}' for {}", name, value, this);
            return Optional.of(this);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") RepositoryException exception) {
            String message = String.format(
                    "Unable to set property '%s' to '%s' for %s", name, value, this
            );
            log.error(message, exception);
            return Optional.empty();
        }
    }

    @SneakyThrows
    private Optional<Binary> asBinary(Value value) {
        int valueType = value.getType();
        if (valueType == PropertyType.BINARY) {
            Binary binary = value.getBinary();
            return Optional.of(binary);
        } else {
            log.trace("Not a binary type");
            return Optional.empty();
        }
    }

    @SneakyThrows
    private File asFile(Binary binary) {
        log.trace("Converting binary to a file");
        File tempFile = File.createTempFile("jcr-binary_", ".tmp");
        tempFile.deleteOnExit();
        Path tempFilePath = tempFile.toPath();
        try (InputStream inputStream = binary.getStream();
             OutputStream outputStream = Files.newOutputStream(tempFilePath)) {
            IOUtils.copy(inputStream, outputStream);
            binary.dispose();
        }
        log.trace("Converted binary to a file: {}", tempFile);
        return tempFile;
    }
}
