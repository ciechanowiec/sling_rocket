package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.WithJCRPath;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static eu.ciechanowiec.sneakyfun.SneakyFunction.sneaky;

/**
 * {@link Property}-ies of an existing {@link Node}.
 */
@SuppressWarnings(
    {
        "WeakerAccess", "ClassWithTooManyMethods", "MethodCount", "MultipleStringLiterals",
        "PMD.AvoidDuplicateLiterals", "PMD.ExcessivePublicCount",
        "PMD.TooManyMethods", "PMD.LinguisticNaming", "PMD.CouplingBetweenObjects"
    }
)
@Slf4j
@ToString
public class NodeProperties implements WithJCRPath {

    private final JCRPath jcrPath;
    @ToString.Exclude
    private final Supplier<Optional<ResourceAccess>> resourceAccessSupplier;
    @ToString.Exclude
    private final Supplier<Optional<ResourceResolver>> resourceResolverSupplier;

    /**
     * Constructs an instance of this class using a {@link ResourceAccess} object to manage {@link ResourceResolver}
     * instances.
     * <p>
     * This constructor is designed for scenarios where the lifecycle of the {@link ResourceResolver} is managed by this
     * object. The provided {@link ResourceAccess} is used to acquire and subsequently close a {@link ResourceResolver}
     * for each {@link Repository} operation.
     * <p>
     * This object will assume ownership of the {@link ResourceResolver} it acquires and will close it automatically.
     * However, this might introduce a performance overhead due to repeated acquisitions. For performance-sensitive code
     * sections where a {@link ResourceResolver} is already available, use
     * {@link NodeProperties#NodeProperties(JCRPath, ResourceResolver)}.
     *
     * @param jcrNodePath    {@link JCRPath} to the underlying existing {@link Node}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     */
    public NodeProperties(JCRPath jcrNodePath, ResourceAccess resourceAccess) {
        this.jcrPath = jcrNodePath;
        resourceResolverSupplier = Optional::empty;
        resourceAccessSupplier = () -> Optional.ofNullable(resourceAccess);
        log.trace("Initialized {}", this);
    }

    /**
     * Constructs an instance of this class using a {@link ResourceAccess} object to manage {@link ResourceResolver}
     * instances.
     * <p>
     * This constructor is designed for scenarios where the lifecycle of the {@link ResourceResolver} is managed by this
     * object. The provided {@link ResourceAccess} is used to acquire and subsequently close a {@link ResourceResolver}
     * for each {@link Repository} operation.
     * <p>
     * This object will assume ownership of the {@link ResourceResolver} it acquires and will close it automatically.
     * However, this might introduce a performance overhead due to repeated acquisitions. For performance-sensitive code
     * sections where a {@link ResourceResolver} is already available, use
     * {@link NodeProperties#NodeProperties(JCRPath, ResourceResolver)}.
     *
     * @param withJCRPath    object that contains a {@link JCRPath} to the underlying {@link Node}
     * @param resourceAccess {@link ResourceAccess} that will be used by the constructed object to acquire access to
     *                       resources
     */
    public NodeProperties(WithJCRPath withJCRPath, ResourceAccess resourceAccess) {
        this(withJCRPath.jcrPath(), resourceAccess);
    }

    /**
     * Constructs an instance of this class utilizing an externally provided, pre-existing {@link ResourceResolver}.
     * <p>
     * This constructor is designed for scenarios where the lifecycle of the {@link ResourceResolver} is managed by the
     * calling context (e.g., Sling). The provided {@link ResourceResolver} is used for all subsequent
     * {@link Repository} operations within this object. Consequently, this object will <strong>not</strong> assume
     * ownership of the {@link ResourceResolver} and will not attempt to close it.
     * <p>
     * The object constructed with this constructor offers superior performance compared to
     * {@link NodeProperties#NodeProperties(JCRPath, ResourceAccess)} and
     * {@link NodeProperties#NodeProperties(WithJCRPath, ResourceAccess)} as it avoids the need to repeatedly acquire
     * and authenticate a new {@link ResourceResolver} for each {@link Repository} operation. It is therefore the
     * preferred choice in performance-sensitive code sections where a {@link ResourceResolver} is already available.
     *
     * @param jcrNodePath      {@link JCRPath} to the underlying existing {@link Node}
     * @param resourceResolver {@link ResourceResolver} that will be used by the constructed object to acquire access to
     *                         resources
     */
    public NodeProperties(JCRPath jcrNodePath, ResourceResolver resourceResolver) {
        this.jcrPath = jcrNodePath;
        resourceResolverSupplier = () -> Optional.ofNullable(resourceResolver);
        resourceAccessSupplier = Optional::empty;
        log.trace("Initialized {}", this);
    }

    /**
     * Constructs an instance of this class utilizing an externally provided, pre-existing {@link ResourceResolver} from
     * the passed {@link Resource}.
     * <p>
     * This constructor is designed for scenarios where the lifecycle of the {@link ResourceResolver} is managed by the
     * calling context (e.g., Sling). The {@link ResourceResolver} from the passed {@link Resource} is used for all
     * subsequent {@link Repository} operations within this object. Consequently, this object will <strong>not</strong>
     * assume ownership of the {@link ResourceResolver} and will not attempt to close it.
     * <p>
     * The object constructed with this constructor offers superior performance compared to
     * {@link NodeProperties#NodeProperties(JCRPath, ResourceAccess)} and
     * {@link NodeProperties#NodeProperties(WithJCRPath, ResourceAccess)} as it avoids the need to repeatedly acquire
     * and authenticate a new {@link ResourceResolver} for each {@link Repository} operation. It is therefore the
     * preferred choice in performance-sensitive code sections where a {@link ResourceResolver} is already available.
     *
     * @param resource {@link Resource} that represents the underlying existing {@link Node} and whose
     *                 {@link ResourceResolver} will be used by the constructed object to acquire access to resources
     */
    public NodeProperties(Resource resource) {
        this(new TargetJCRPath(resource.getPath()), resource.getResourceResolver());
    }

    /**
     * Retrieves the primary type of the underlying {@link Node}.
     *
     * @return primary type of the underlying {@link Node}
     */
    public String primaryType() {
        return resourceResolverSupplier.get()
            .map(this::primaryType)
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return primaryType(resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private String primaryType(ResourceResolver resourceResolver) {
        log.trace("Retrieving the primary type of {}", this);
        String jcrPathRaw = jcrPath.get();
        return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(sneaky(node -> Optional.ofNullable(node.getPrimaryNodeType())))
            .map(NodeType::getName)
            .orElseThrow();
    }

    /**
     * Checks if the underlying {@link Node} is of one of the specified primary types.
     *
     * @param acceptablePrimaryTypes names of the primary types against which the check is performed
     * @return {@code true} if the underlying {@link Node} is of one of the specified primary types; {@code false}
     * otherwise
     */
    public boolean isPrimaryType(String... acceptablePrimaryTypes) {
        return isPrimaryType(List.of(acceptablePrimaryTypes));
    }

    /**
     * Checks if the underlying {@link Node} is of one of the specified primary types.
     *
     * @param acceptablePrimaryTypes names of the primary types against which the check is performed
     * @return {@code true} if the underlying {@link Node} is of one of the specified primary types; {@code false}
     * otherwise
     */
    public boolean isPrimaryType(Collection<String> acceptablePrimaryTypes) {
        log.trace("Checking if {} is of this primary type: '{}'", this, acceptablePrimaryTypes);
        String actualPrimaryType = primaryType();
        return acceptablePrimaryTypes.stream().anyMatch(actualPrimaryType::equals);
    }

    /**
     * Asserts that the underlying {@link Node} is of the specified primary type.
     *
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
     *
     * @param propertyName name of the {@link Property} from which the {@link Value} should be retrieved
     * @param defaultValue default {@link Value} to use if the specified {@link Property} does not exist or its value
     *                     cannot be converted to the requested type; the {@code defaultValue} is also used to define
     *                     the type to convert the {@link Value} to
     * @param <T>          expected type
     * @return {@link Value} of the specified {@link Property} converted to the requested type or the
     * {@code defaultValue} if the specified {@link Property} does not exist or its {@link Value} cannot be converted to
     * the requested type
     */
    public <T> T propertyValue(String propertyName, T defaultValue) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> propertyValue(propertyName, defaultValue, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return propertyValue(propertyName, defaultValue, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private <T> T propertyValue(String propertyName, T defaultValue, ResourceResolver resourceResolver) {
        log.trace("Getting '{}' property value for {}", propertyName, this);
        String jcrPathRaw = jcrPath.get();
        return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .map(Resource::getValueMap)
            .map(WithPrimitiveArrayTranslation::new)
            .map(valueMapWithTranslation -> valueMapWithTranslation.get(propertyName, defaultValue))
            .orElse(defaultValue);
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
     *
     * @param propertyName name of the {@link Property} from which the {@link Value} should be retrieved
     * @param type         {@link Class} with the type that should represent the requested {@link Value} and which that
     *                     {@link Value} will be cast to
     * @param <T>          expected type
     * @return {@link Optional} containing the {@link Value} of the specified {@link Property} converted to the
     * requested type; if the specified {@link Property} does not exist or its {@link Value} cannot be converted to the
     * requested type, an empty {@link Optional} is returned
     */
    public <T> Optional<T> propertyValue(String propertyName, Class<T> type) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> propertyValue(propertyName, type, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return propertyValue(propertyName, type, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private <T> Optional<T> propertyValue(String propertyName, Class<T> type, ResourceResolver resourceResolver) {
        log.trace("Getting '{}' property value for {}", propertyName, this);
        String jcrPathRaw = jcrPath.get();
        return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .map(Resource::getValueMap)
            .map(WithPrimitiveArrayTranslation::new)
            .flatMap(valueMapWithTranslation -> valueMapWithTranslation.get(propertyName, type));
    }

    /**
     * Retrieves the {@link PropertyType} of the specified {@link Property}.
     *
     * @param propertyName name of the {@link Property} whose {@link PropertyType} should be retrieved
     * @return {@link PropertyType} of the specified {@link Property}
     */
    public int propertyType(String propertyName) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> propertyType(propertyName, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return propertyType(propertyName, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private int propertyType(String propertyName, ResourceResolver resourceResolver) {
        log.trace("Getting '{}' property type for {}", propertyName, this);
        String jcrPathRaw = jcrPath.get();
        return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> new ConditionalProperty(propertyName).retrieveFrom(node))
            .map(this::firstValue)
            .map(Value::getType)
            .orElse(PropertyType.UNDEFINED);
    }

    @SneakyThrows
    private Value firstValue(Property property) {
        return Conditional.conditional(property.isMultiple())
            .onTrue(() -> property.getValues()[NumberUtils.INTEGER_ZERO])
            .onFalse(property::getValue)
            .get(Value.class);
    }

    /**
     * Checks if the underlying {@link Node} contains a {@link Property} that has the specified name.
     *
     * @param propertyName name of the hypothetically existent {@link Property}
     * @return {@code true} if the underlying {@link Node} contains a {@link Property} that has the specified name;
     * {@code false} otherwise
     */
    public boolean containsProperty(String propertyName) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> containsProperty(propertyName, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return containsProperty(propertyName, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private boolean containsProperty(String propertyName, ResourceResolver resourceResolver) {
        log.trace("Checking if '{}' contains property of this name: '{}'", this, propertyName);
        String jcrPathRaw = jcrPath.get();
        return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .map(Resource::getValueMap)
            .map(valueMap -> valueMap.containsKey(propertyName))
            .orElse(false);
    }

    /**
     * Retrieves the {@link Value} of a {@link Property} of type {@link PropertyType#BINARY} as an
     * {@link InputStreamWithDataSize}.
     *
     * @param propertyName name of the {@link Property} from which the {@link Value} of type {@link PropertyType#BINARY}
     *                     should be retrieved; in most cases it will be {@link JcrConstants#JCR_DATA}
     * @return {@link Value} of a {@link Property} of type {@link PropertyType#BINARY} as an {@link InputStream}; empty
     * {@link Optional} is returned if the {@link Property} isn't of type {@link PropertyType#BINARY} or doesn't exist
     */
    @SneakyThrows
    public InputStreamWithDataSize retrieveBinary(String propertyName) {
        log.trace("Getting the value of the '{}' property as a file. {}", propertyName, this);
        return resourceResolverSupplier.get()
            .map(resourceResolver -> new InputStreamWithDataSize(jcrPath, propertyName, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> new InputStreamWithDataSize(jcrPath, propertyName, resourceAccess)
                    )
            ).orElseThrow();
    }

    /**
     * Retrieves the {@link DataSize} of a {@link Value} of a {@link Property} of type {@link PropertyType#BINARY}.
     *
     * @param propertyName name of the {@link Property} of type {@link PropertyType#BINARY} that contains a
     *                     {@link Value} whose {@link DataSize} should be retrieved
     * @return {@link DataSize} of a {@link Value} of a {@link Property} of type {@link PropertyType#BINARY}; zero-sized
     * {@link DataSize} is returned if the {@link Property} isn't of type {@link PropertyType#BINARY} or doesn't exist
     */
    public DataSize binarySize(String propertyName) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> binarySize(propertyName, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return binarySize(propertyName, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private DataSize binarySize(String propertyName, ResourceResolver resourceResolver) {
        log.trace("Checking the size of the '{}' binary property. {}", propertyName, this);
        String jcrPathRaw = jcrPath.get();
        return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> new ConditionalProperty(propertyName).retrieveFrom(node))
            .map(this::firstValue)
            .flatMap(this::asBinary)
            .map(sneaky(Binary::getSize))
            .map(bytes -> new DataSize(bytes, DataUnit.BYTES))
            .orElse(new DataSize(0, DataUnit.BYTES));
    }

    /**
     * <p>
     * Returns all {@link Property}-ies of the underlying {@link Node} as a {@link Map} of {@link Property} names to
     * {@link Property} {@link Value}-s converted to {@link String}.
     * </p>
     * The following {@link Property}-ies are omitted from the result:
     * <ol>
     *     <li>{@link Property}-ies with {@link Value}-s that cannot be converted to {@link String}</li>
     *     <li>{@link Property}-ies of type {@link PropertyType#BINARY}</li>
     * </ol>
     *
     * @return {@link Property}-ies of the underlying {@link Node} as a {@link Map} of {@link Property} names to
     * {@link Property} {@link Value}-s converted to {@link String}
     */
    public Map<String, String> all() {
        return resourceResolverSupplier.get()
            .map(this::all)
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return all(resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private Map<String, String> all(ResourceResolver resourceResolver) {
        log.trace("Retrieving all properties of {}", this);
        String jcrPathRaw = jcrPath.get();
        return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .map(Resource::getValueMap)
            .map(ValueMap::keySet)
            .orElse(Set.of())
            .stream()
            .filter(propertyName -> propertyType(propertyName) != PropertyType.BINARY)
            .map(propertyName -> Map.entry(
                propertyName, propertyValue(propertyName, DefaultProperties.STRING_CLASS))
            )
            .filter(entry -> entry.getValue().isPresent())
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().orElseThrow()))
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first)
            );
    }

    /**
     * Sets the specified {@link Property}-ies for the underlying {@link Node} according to the logic described in the
     * respective method for a given type of {@link Value}:
     * <ol>
     *     <li>{@link Node#setProperty(String, String)}</li>
     *     <li>{@link Node#setProperty(String, boolean)}</li>
     *     <li>{@link Node#setProperty(String, long)}</li>
     *     <li>{@link Node#setProperty(String, double)}</li>
     *     <li>{@link Node#setProperty(String, BigDecimal)}</li>
     *     <li>{@link Node#setProperty(String, Calendar)}</li>
     * </ol>
     * All properties are set transactionally and atomically, i.e. all the properties are set at the same time
     * or none of them is set.
     * <p>
     * This operation can overwrite the current {@link PropertyType} of the existing {@link Property} if it differs from
     * the new {@link PropertyType}. However, such overwriting behavior is supported only to the extent to which
     * it is supported by the underlying and respective {@link Node#setProperty(String, String)},
     * {@link Node#setProperty(String, boolean)}, {@link Node#setProperty(String, boolean)},
     * {@link Node#setProperty(String, long)}, {@link Node#setProperty(String, double)},
     * {@link Node#setProperty(String, BigDecimal)} or {@link Node#setProperty(String, Calendar)}.
     *
     * @param properties {@link Map} of {@link Property}-ies to set, where every key is the name of the {@link Property}
     *                   to set and every value is the value of that {@link Property}; the {@link Class} of the value
     *                   can be only one of the following (if this condition isn't met, the whole operation will fail):
     *                   <ol>
     *                   <li>{@link DefaultProperties#STRING_CLASS}</li>
     *                   <li>{@link DefaultProperties#BOOLEAN_CLASS}</li>
     *                   <li>{@link DefaultProperties#LONG_CLASS}</li>
     *                   <li>{@link DefaultProperties#DOUBLE_CLASS}</li>
     *                   <li>{@link DefaultProperties#DECIMAL_CLASS}</li>
     *                   <li>{@link DefaultProperties#DATE_CLASS}</li>
     *                   </ol>
     * @return {@link Optional} containing this {@link NodeProperties} if all the passed {@link Property}-ies were set
     * successfully; an empty {@link Optional} is returned if any of the {@link Property}-ies wasn't set due to any
     * reason
     */
    public Optional<NodeProperties> setProperties(Map<String, Object> properties) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> setProperties(properties, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return setProperties(properties, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private Optional<NodeProperties> setProperties(Map<String, Object> properties, ResourceResolver resourceResolver) {
        log.trace("Setting properties '{}' for {}", properties, this);
        String jcrPathRaw = jcrPath.get();
        Optional<NodeProperties> result = Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> setProperties(node, properties));
        result.ifPresent(SneakyConsumer.sneaky(nodeProperties -> resourceResolver.commit()));
        return result;
    }

    private Optional<NodeProperties> setProperties(Node node, Map<String, Object> properties) {
        int expectedNumOfProps = properties.size();
        List<NodeProperties> propsSet = properties.entrySet().stream().map(
                entry -> {
                    String name = entry.getKey();
                    Object value = entry.getValue();
                    return setProperty(node, name, value);
                }
            ).filter(Optional::isPresent)
            .flatMap(Optional::stream)
            .toList();
        int actualNumOfProps = propsSet.size();
        return expectedNumOfProps == actualNumOfProps && actualNumOfProps > NumberUtils.INTEGER_ZERO
            ? Optional.of(propsSet.getFirst())
            : Optional.empty();
    }

    private Optional<NodeProperties> setProperty(Node node, String name, Object value) {
        return switch (value) {
            case String valueString -> setProperty(node, name, valueString);
            case Boolean valueBoolean -> setProperty(node, name, (boolean) valueBoolean);
            case Long valueLong -> setProperty(node, name, (long) valueLong);
            case Double valueDouble -> setProperty(node, name, (double) valueDouble);
            case BigDecimal valueBigDecimal -> setProperty(node, name, valueBigDecimal);
            case Calendar calendar -> setProperty(node, name, calendar);
            default -> {
                log.warn("Unsupported type '{}' for property '{}' for {}", value.getClass(), name, this);
                yield Optional.empty();
            }
        };
    }

    /**
     * <p>
     * Sets the value of the specified {@link Property} according to the logic described in
     * {@link Node#setProperty(String, String)}.
     * </p>
     * This operation can overwrite the current {@link PropertyType} of the existing {@link Property} if it differs from
     * the new {@link PropertyType}. However, such overwriting behavior is supported only to the extent to which it is
     * supported by the underlying {@link Node#setProperty(String, String)}.
     *
     * @param name  name of the {@link Property} to set
     * @param value value of the {@link Property} to set
     * @return {@link Optional} containing this {@link NodeProperties} if the {@link Property} was set successfully; an
     * empty {@link Optional} is returned if the {@link Property} wasn't set due to any reason
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public Optional<NodeProperties> setProperty(String name, String value) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> setProperty(name, value, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return setProperty(name, value, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private Optional<NodeProperties> setProperty(String name, String value, ResourceResolver resourceResolver) {
        log.trace("Setting property '{}' to '{}' for {}", name, value, this);
        String jcrPathRaw = jcrPath.get();
        Optional<NodeProperties> result = Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> setProperty(node, name, value));
        result.ifPresent(SneakyConsumer.sneaky(nodeProperties -> resourceResolver.commit()));
        return result;
    }

    /**
     * <p>
     * Sets the value of the specified {@link Property} according to the logic described in
     * {@link Node#setProperty(String, boolean)}.
     * </p>
     * This operation can overwrite the current {@link PropertyType} of the existing {@link Property} if it differs from
     * the new {@link PropertyType}. However, such overwriting behavior is supported only to the extent to which it is
     * supported by the underlying {@link Node#setProperty(String, boolean)}.
     *
     * @param name  name of the {@link Property} to set
     * @param value value of the {@link Property} to set
     * @return {@link Optional} containing this {@link NodeProperties} if the {@link Property} was set successfully; an
     * empty {@link Optional} is returned if the {@link Property} wasn't set due to any reason
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public Optional<NodeProperties> setProperty(String name, boolean value) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> setProperty(name, value, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return setProperty(name, value, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private Optional<NodeProperties> setProperty(String name, boolean value, ResourceResolver resourceResolver) {
        log.trace("Setting property '{}' to '{}' for {}", name, value, this);
        String jcrPathRaw = jcrPath.get();
        Optional<NodeProperties> result = Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> setProperty(node, name, value));
        result.ifPresent(SneakyConsumer.sneaky(nodeProperties -> resourceResolver.commit()));
        return result;
    }

    /**
     * <p>
     * Sets the value of the specified {@link Property} according to the logic described in
     * {@link Node#setProperty(String, long)}.
     * </p>
     * This operation can overwrite the current {@link PropertyType} of the existing {@link Property} if it differs from
     * the new {@link PropertyType}. However, such overwriting behavior is supported only to the extent to which it is
     * supported by the underlying {@link Node#setProperty(String, long)}.
     *
     * @param name  name of the {@link Property} to set
     * @param value value of the {@link Property} to set
     * @return {@link Optional} containing this {@link NodeProperties} if the {@link Property} was set successfully; an
     * empty {@link Optional} is returned if the {@link Property} wasn't set due to any reason
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public Optional<NodeProperties> setProperty(String name, long value) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> setProperty(name, value, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return setProperty(name, value, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private Optional<NodeProperties> setProperty(String name, long value, ResourceResolver resourceResolver) {
        log.trace("Setting property '{}' to '{}' for {}", name, value, this);
        String jcrPathRaw = jcrPath.get();
        Optional<NodeProperties> result = Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> setProperty(node, name, value));
        result.ifPresent(SneakyConsumer.sneaky(nodeProperties -> resourceResolver.commit()));
        return result;
    }

    /**
     * <p>
     * Sets the value of the specified {@link Property} according to the logic described in
     * {@link Node#setProperty(String, double)}.
     * </p>
     * This operation can overwrite the current {@link PropertyType} of the existing {@link Property} if it differs from
     * the new {@link PropertyType}. However, such overwriting behavior is supported only to the extent to which it is
     * supported by the underlying {@link Node#setProperty(String, double)}.
     *
     * @param name  name of the {@link Property} to set
     * @param value value of the {@link Property} to set
     * @return {@link Optional} containing this {@link NodeProperties} if the {@link Property} was set successfully; an
     * empty {@link Optional} is returned if the {@link Property} wasn't set due to any reason
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public Optional<NodeProperties> setProperty(String name, double value) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> setProperty(name, value, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return setProperty(name, value, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private Optional<NodeProperties> setProperty(String name, double value, ResourceResolver resourceResolver) {
        log.trace("Setting property '{}' to '{}' for {}", name, value, this);
        String jcrPathRaw = jcrPath.get();
        Optional<NodeProperties> result = Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> setProperty(node, name, value));
        result.ifPresent(SneakyConsumer.sneaky(nodeProperties -> resourceResolver.commit()));
        return result;
    }

    /**
     * <p>
     * Sets the value of the specified {@link Property} according to the logic described in
     * {@link Node#setProperty(String, BigDecimal)}.
     * </p>
     * This operation can overwrite the current {@link PropertyType} of the existing {@link Property} if it differs from
     * the new {@link PropertyType}. However, such overwriting behavior is supported only to the extent to which it is
     * supported by the underlying {@link Node#setProperty(String, BigDecimal)}.
     *
     * @param name  name of the {@link Property} to set
     * @param value value of the {@link Property} to set
     * @return {@link Optional} containing this {@link NodeProperties} if the {@link Property} was set successfully; an
     * empty {@link Optional} is returned if the {@link Property} wasn't set due to any reason
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public Optional<NodeProperties> setProperty(String name, BigDecimal value) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> setProperty(name, value, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return setProperty(name, value, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private Optional<NodeProperties> setProperty(String name, BigDecimal value, ResourceResolver resourceResolver) {
        log.trace("Setting property '{}' to '{}' for {}", name, value, this);
        String jcrPathRaw = jcrPath.get();
        Optional<NodeProperties> result = Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> setProperty(node, name, value));
        result.ifPresent(SneakyConsumer.sneaky(nodeProperties -> resourceResolver.commit()));
        return result;
    }

    /**
     * <p>
     * Sets the value of the specified {@link Property} according to the logic described in
     * {@link Node#setProperty(String, Calendar)}.
     * </p>
     * This operation can overwrite the current {@link PropertyType} of the existing {@link Property} if it differs from
     * the new {@link PropertyType}. However, such overwriting behavior is supported only to the extent to which it is
     * supported by the underlying {@link Node#setProperty(String, Calendar)}.
     *
     * @param name  name of the {@link Property} to set
     * @param value value of the {@link Property} to set
     * @return {@link Optional} containing this {@link NodeProperties} if the {@link Property} was set successfully; an
     * empty {@link Optional} is returned if the {@link Property} wasn't set due to any reason
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public Optional<NodeProperties> setProperty(String name, Calendar value) {
        return resourceResolverSupplier.get()
            .map(resourceResolver -> setProperty(name, value, resourceResolver))
            .or(
                () -> resourceAccessSupplier.get()
                    .map(
                        resourceAccess -> {
                            try (ResourceResolver resourceResolver = resourceAccess.acquireAccess()) {
                                return setProperty(name, value, resourceResolver);
                            }
                        }
                    )
            ).orElseThrow();
    }

    private Optional<NodeProperties> setProperty(String name, Calendar value, ResourceResolver resourceResolver) {
        log.trace("Setting property '{}' to '{}' for {}", name, value, this);
        String jcrPathRaw = jcrPath.get();
        Optional<NodeProperties> result = Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> setProperty(node, name, value));
        result.ifPresent(SneakyConsumer.sneaky(nodeProperties -> resourceResolver.commit()));
        return result;
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    private Optional<NodeProperties> setProperty(Node node, String name, String value) {
        try {
            node.setProperty(name, value);
            log.trace("Property '{}' set to '{}' for {}", name, value, this);
            return Optional.of(this);
        } catch (
            @SuppressWarnings("OverlyBroadCatchBlock")
            RepositoryException exception
        ) {
            String message = "Unable to set property '%s' to '%s' for %s".formatted(name, value, this);
            log.error(message, exception);
            return Optional.empty();
        }
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    private Optional<NodeProperties> setProperty(Node node, String name, boolean value) {
        try {
            node.setProperty(name, value);
            log.trace("Property '{}' set to '{}' for {}", name, value, this);
            return Optional.of(this);
        } catch (
            @SuppressWarnings("OverlyBroadCatchBlock")
            RepositoryException exception
        ) {
            String message = "Unable to set property '%s' to '%s' for %s".formatted(name, value, this);
            log.error(message, exception);
            return Optional.empty();
        }
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    private Optional<NodeProperties> setProperty(Node node, String name, long value) {
        try {
            node.setProperty(name, value);
            log.trace("Property '{}' set to '{}' for {}", name, value, this);
            return Optional.of(this);
        } catch (
            @SuppressWarnings("OverlyBroadCatchBlock")
            RepositoryException exception
        ) {
            String message = "Unable to set property '%s' to '%s' for %s".formatted(name, value, this);
            log.error(message, exception);
            return Optional.empty();
        }
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    private Optional<NodeProperties> setProperty(Node node, String name, double value) {
        try {
            node.setProperty(name, value);
            log.trace("Property '{}' set to '{}' for {}", name, value, this);
            return Optional.of(this);
        } catch (
            @SuppressWarnings("OverlyBroadCatchBlock")
            RepositoryException exception
        ) {
            String message = "Unable to set property '%s' to '%s' for %s".formatted(name, value, this);
            log.error(message, exception);
            return Optional.empty();
        }
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    private Optional<NodeProperties> setProperty(Node node, String name, BigDecimal value) {
        try {
            node.setProperty(name, value);
            log.trace("Property '{}' set to '{}' for {}", name, value, this);
            return Optional.of(this);
        } catch (
            @SuppressWarnings("OverlyBroadCatchBlock")
            RepositoryException exception
        ) {
            String message = "Unable to set property '%s' to '%s' for %s".formatted(name, value, this);
            log.error(message, exception);
            return Optional.empty();
        }
    }

    @SuppressWarnings("PMD.LinguisticNaming")
    private Optional<NodeProperties> setProperty(Node node, String name, Calendar value) {
        try {
            node.setProperty(name, value);
            log.trace("Property '{}' set to '{}' for {}", name, value, this);
            return Optional.of(this);
        } catch (
            @SuppressWarnings("OverlyBroadCatchBlock")
            RepositoryException exception
        ) {
            String message = "Unable to set property '%s' to '%s' for %s".formatted(name, value, this);
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

    @Override
    public JCRPath jcrPath() {
        return jcrPath;
    }
}
