package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetMetadata;
import eu.ciechanowiec.sling.rocket.asset.FileMetadata;
import eu.ciechanowiec.sling.rocket.asset.StagedAssetReal;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jcr.PropertyType;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"MagicNumber", "MultipleStringLiterals", "MethodLength", "PMD.AvoidDuplicateLiterals"})
class NodePropertiesTest extends TestEnvironment {

    private Calendar unix1980;
    private Calendar unix1990;
    private File file;

    NodePropertiesTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        unix1980 = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        unix1990 = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        unix1980.clear();
        unix1990.clear();
        unix1980.set(1980, Calendar.JANUARY, 1, 0, 0, 0);
        unix1990.set(1990, Calendar.JANUARY, 1, 0, 0, 0);
        unix1980.set(Calendar.MILLISECOND, 0);
        unix1990.set(Calendar.MILLISECOND, 0);
        unix1980.setLenient(false);
        unix1990.setLenient(false);
        file = loadResourceIntoFile();
    }

    @SneakyThrows
    private File loadResourceIntoFile() {
        File createdFile = File.createTempFile("jcr-binary_", ".tmp");
        createdFile.deleteOnExit();
        Path tempFilePath = createdFile.toPath();
        Thread currentThread = Thread.currentThread();
        ClassLoader classLoader = currentThread.getContextClassLoader();
        try (
                InputStream inputStream = Optional.ofNullable(
                        classLoader.getResourceAsStream("1.jpeg")
                ).orElseThrow();
                OutputStream outputStream = Files.newOutputStream(tempFilePath)
        ) {
            IOUtils.copy(inputStream, outputStream);
        }
        assertTrue(createdFile.exists());
        return createdFile;
    }

    @Test
    void mustSetPropertyGeneral() {
        context.build().resource("/content",
                Map.of(
                        "stringus-namus", "stringus-valus",
                        "booleanus-namus", true,
                        "longus-namus", 123L,
                        "doubleus-namus", 99.99,
                        "decimalus-namus", new BigDecimal("999.99"),
                        "calendarus-namus", unix1980
                )
        ).commit();
        Map<String, String> initialMap = Map.of(
                "stringus-namus", "stringus-valus",
                "booleanus-namus", "true",
                "longus-namus", "123",
                "doubleus-namus", "99.99",
                "decimalus-namus", "999.99",
                "calendarus-namus", "1980-01-01T00:00:00.000Z",
                "jcr:primaryType", "nt:unstructured"
        );
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        Map<String, String> firstActualMap = nodeProperties.all();
        assertEquals(initialMap, firstActualMap);
        nodeProperties.setProperty("stringus-namus", "stringus-valus-2");
        nodeProperties.setProperty("booleanus-namus", "false");
        nodeProperties.setProperty("longus-namus", "321");
        nodeProperties.setProperty("doubleus-namus", "88.88");
        nodeProperties.setProperty("decimalus-namus", "888.88");
        NodeProperties newNodeProperties = nodeProperties.setProperty("calendarus-namus", "1990-01-01T00:00:00.000Z")
                .orElseThrow();
        Map<String, String> secondActualMap = newNodeProperties.all();
        Map<String, String> finalMap = Map.of(
                "stringus-namus", "stringus-valus-2",
                "booleanus-namus", "false",
                "longus-namus", "321",
                "doubleus-namus", "88.88",
                "decimalus-namus", "888.88",
                "calendarus-namus", "1990-01-01T00:00:00.000Z",
                "jcr:primaryType", "nt:unstructured"
        );
        assertEquals(finalMap, secondActualMap);
    }

    @Test
    void mustSetPropertiesOfTypes() {
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        assertAll(
                () -> assertEquals(PropertyType.UNDEFINED, nodeProperties.propertyType("stringus-namus")),
                () -> assertEquals(PropertyType.UNDEFINED, nodeProperties.propertyType("booleanus-namus")),
                () -> assertEquals(PropertyType.UNDEFINED, nodeProperties.propertyType("longus-namus")),
                () -> assertEquals(PropertyType.UNDEFINED, nodeProperties.propertyType("doubleus-namus")),
                () -> assertEquals(PropertyType.UNDEFINED, nodeProperties.propertyType("decimalus-namus")),
                () -> assertEquals(PropertyType.UNDEFINED, nodeProperties.propertyType("calendarus-namus"))
        );
        context.build().resource("/content").commit();
        nodeProperties.setProperty("stringus-namus", "stringus-valus").orElseThrow();
        nodeProperties.setProperty("booleanus-namus", true).orElseThrow();
        nodeProperties.setProperty("longus-namus", 123L).orElseThrow();
        nodeProperties.setProperty("doubleus-namus", 99.99).orElseThrow();
        nodeProperties.setProperty("decimalus-namus", new BigDecimal("999.99")).orElseThrow();
        NodeProperties firstResult = nodeProperties.setProperty("calendarus-namus", unix1980).orElseThrow();
        assertAll(
                () -> assertEquals(PropertyType.STRING, firstResult.propertyType("stringus-namus")),
                () -> assertEquals(PropertyType.BOOLEAN, firstResult.propertyType("booleanus-namus")),
                () -> assertEquals(PropertyType.LONG, firstResult.propertyType("longus-namus")),
                () -> assertEquals(PropertyType.DOUBLE, firstResult.propertyType("doubleus-namus")),
                () -> assertEquals(PropertyType.DECIMAL, firstResult.propertyType("decimalus-namus")),
                () -> assertEquals(PropertyType.DATE, firstResult.propertyType("calendarus-namus")),
                () -> assertEquals(PropertyType.UNDEFINED, firstResult.propertyType("unknown"))
        );
        firstResult.setProperty("stringus-namus", true).orElseThrow();
        firstResult.setProperty("booleanus-namus", "true").orElseThrow();
        firstResult.setProperty("longus-namus", "123").orElseThrow();
        firstResult.setProperty("doubleus-namus", "99.99").orElseThrow();
        firstResult.setProperty("decimalus-namus", "999.99").orElseThrow();
        NodeProperties secondResult = nodeProperties.setProperty("calendarus-namus", "1980-01-01T00:00:00.000Z")
                                                    .orElseThrow();
        assertAll(
                () -> assertEquals(PropertyType.BOOLEAN, secondResult.propertyType("stringus-namus")),
                () -> assertEquals(PropertyType.STRING, secondResult.propertyType("booleanus-namus")),
                () -> assertEquals(PropertyType.STRING, secondResult.propertyType("longus-namus")),
                () -> assertEquals(PropertyType.STRING, secondResult.propertyType("doubleus-namus")),
                () -> assertEquals(PropertyType.STRING, secondResult.propertyType("decimalus-namus")),
                () -> assertEquals(PropertyType.STRING, secondResult.propertyType("calendarus-namus")),
                () -> assertEquals(PropertyType.UNDEFINED, secondResult.propertyType("unknown"))
        );
    }

    @Test
    void mustExcludeBinariesFromAll() {
        TargetJCRPath realAssetPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        new StagedAssetReal(() -> Optional.of(file), new AssetMetadata() {
            @Override
            public String mimeType() {
                return "image/jpeg";
            }

            @Override
            public Map<String, String> all() {
                return Map.of(PN_MIME_TYPE, mimeType(), "originalFileName", "originalus");
            }

            @Override
            public Optional<NodeProperties> properties() {
                return Optional.empty();
            }
        }, resourceAccess).save(realAssetPath);
        TargetJCRPath ntFilePath = new TargetJCRPath(new ParentJCRPath(realAssetPath), Asset.FILE_NODE_NAME);
        TargetJCRPath ntResourcePath = new TargetJCRPath(new ParentJCRPath(ntFilePath), JcrConstants.JCR_CONTENT);
        NodeProperties nodeProperties = new NodeProperties(ntResourcePath, resourceAccess);
        assertAll(
                () -> assertTrue(nodeProperties.isPrimaryType(JcrConstants.NT_RESOURCE)),
                () -> assertEquals(5, nodeProperties.all().size())
        );
    }

    @Test
    void mustShowPropertyType() {
        context.build().resource("/content",
                Map.of(
                        "stringus-namus", "stringus-valus",
                        "booleanus-namus", true,
                        "longus-namus", 123L,
                        "doubleus-namus", 99.99,
                        "decimalus-namus", new BigDecimal("999.99"),
                        "calendarus-namus", unix1980
                )
        ).commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        assertAll(
                () -> assertEquals(PropertyType.STRING, nodeProperties.propertyType("stringus-namus")),
                () -> assertEquals(PropertyType.BOOLEAN, nodeProperties.propertyType("booleanus-namus")),
                () -> assertEquals(PropertyType.LONG, nodeProperties.propertyType("longus-namus")),
                () -> assertEquals(PropertyType.DOUBLE, nodeProperties.propertyType("doubleus-namus")),
                () -> assertEquals(PropertyType.DECIMAL, nodeProperties.propertyType("decimalus-namus")),
                () -> assertEquals(PropertyType.DATE, nodeProperties.propertyType("calendarus-namus")),
                () -> assertEquals(PropertyType.UNDEFINED, nodeProperties.propertyType("unknown"))
        );
        NodeProperties nonExistentNodeProperties = new NodeProperties(
                new TargetJCRPath("/non-existent"), resourceAccess
        );
        assertEquals(PropertyType.UNDEFINED, nonExistentNodeProperties.propertyType("unknown"));
    }

    @Test
    void mustNotSetPropertyWhenNoPath() {
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/non-existent"), resourceAccess);
        Optional<NodeProperties> newNodeProperties = nodeProperties.setProperty("stringus-namus", "stringus-valus");
        Map<String, String> allProps = nodeProperties.all();
        assertAll(
                () -> assertTrue(newNodeProperties.isEmpty()),
                () -> assertTrue(allProps.isEmpty())
        );
    }

    @Test
    void mustNotSetIllegalProperty() {
        context.build().resource("/content", Map.of()).commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        Map<String, String> initialAll = nodeProperties.all();
        Optional<NodeProperties> newNodeProperties = nodeProperties.setProperty(
                JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_ORDERED_FOLDER
        );
        Map<String, String> finalAll = nodeProperties.all();
        assertAll(
                () -> assertTrue(newNodeProperties.isEmpty()),
                () -> assertEquals(initialAll, finalAll)
        );
    }

    @Test
    void mustGetAll() {
        context.build().resource("/content",
                Map.of(
                        "stringus-namus", "stringus-valus",
                        "booleanus-namus", true,
                        "longus-namus", 123L,
                        "doubleus-namus", 99.99,
                        "decimalus-namus", new BigDecimal("999.99"),
                        "calendarus-namus", unix1980
                )
        ).commit();
        Map<String, String> expectedMap = Map.of(
                "stringus-namus", "stringus-valus",
                "booleanus-namus", "true",
                "longus-namus", "123",
                "doubleus-namus", "99.99",
                "decimalus-namus", "999.99",
                "calendarus-namus", "1980-01-01T00:00:00.000Z",
                "jcr:primaryType", "nt:unstructured"
        );
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        Map<String, String> actualMap = nodeProperties.all();
        assertEquals(expectedMap, actualMap);
    }

    @Test
    void mustGetDefaultAndCustomUnaryValues() {
        context.build().resource("/content",
                Map.of(
                        "stringus-namus", "stringus-valus",
                        "booleanus-namus", true,
                        "longus-namus", 123L,
                        "doubleus-namus", 99.99,
                        "decimalus-namus", new BigDecimal("999.99"),
                        "calendarus-namus", unix1980
                )
        ).commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        assertAll(
                () -> {
                    String actualString = nodeProperties.propertyValue(
                            "stringus-namus", DefaultProperties.STRING_EMPTY
                    );
                    assertEquals("stringus-valus", actualString);
                },
                () -> {
                    boolean actualBoolean = nodeProperties.propertyValue(
                            "booleanus-namus", DefaultProperties.BOOLEAN_FALSE
                    );
                    assertTrue(actualBoolean);
                },
                () -> {
                    Boolean actualBoolean = nodeProperties.propertyValue(
                            "booleanus-namus", Boolean.FALSE
                    );
                    assertTrue(actualBoolean);
                },
                () -> {
                    long actualLong = nodeProperties.propertyValue(
                            "longus-namus", DefaultProperties.LONG_ZERO
                    );
                    assertEquals(123L, actualLong);
                },
                () -> {
                    Long actualLong = nodeProperties.propertyValue(
                            "longus-namus", NumberUtils.LONG_ZERO
                    );
                    assertEquals(123L, actualLong);
                },
                () -> {
                    double actualDouble = nodeProperties.propertyValue(
                            "doubleus-namus", DefaultProperties.DOUBLE_ZERO
                    );
                    assertEquals(99.99, actualDouble);
                },
                () -> {
                    Double actualDouble = nodeProperties.propertyValue(
                            "doubleus-namus", NumberUtils.DOUBLE_ZERO
                    );
                    assertEquals(99.99, actualDouble);
                },
                () -> {
                    BigDecimal actualDecimal = nodeProperties.propertyValue(
                            "decimalus-namus", DefaultProperties.DECIMAL_ZERO
                    );
                    assertEquals(new BigDecimal("999.99"), actualDecimal);
                },
                () -> {
                    Calendar actualCalendar = nodeProperties.propertyValue(
                            "calendarus-namus", DefaultProperties.DATE_UNIX_EPOCH
                    );
                    assertEquals(unix1980, actualCalendar);
                }
        );
    }

    @Test
    void mustGetDefaultAndCustomMultiValues() {
        context.build().resource("/content",
                Map.of(
                        "stringus-namus", new String[]{"stringus-valus-1", "stringus-valus-2"},
                        "booleanus-namus", new boolean[]{true, true},
                        "longus-namus", new long[]{123L, 124L},
                        "doubleus-namus", new double[]{99.99, 199.99},
                        "decimalus-namus", new BigDecimal[]{
                                new BigDecimal("999.99"), new BigDecimal("1999.99")
                        },
                        "calendarus-namus", new Calendar[]{unix1980, unix1990}
                )
        ).commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        assertAll(
                () -> {
                    String[] actualString = nodeProperties.propertyValue(
                            "stringus-namus", new String[]{DefaultProperties.STRING_EMPTY}
                    );
                    assertArrayEquals(new String[]{"stringus-valus-1", "stringus-valus-2"}, actualString);
                },
                () -> {
                    boolean[] actualBoolean = nodeProperties.propertyValue(
                            "booleanus-namus", new boolean[]{DefaultProperties.BOOLEAN_FALSE}
                    );
                    assertArrayEquals(new boolean[]{true, true}, actualBoolean);
                },
                () -> {
                    Boolean[] actualBoolean = nodeProperties.propertyValue(
                            "booleanus-namus", new Boolean[]{DefaultProperties.BOOLEAN_FALSE}
                    );
                    assertArrayEquals(new Boolean[]{true, true}, actualBoolean);
                },
                () -> {
                    long[] actualLong = nodeProperties.propertyValue(
                            "longus-namus", new long[]{DefaultProperties.LONG_ZERO}
                    );
                    assertArrayEquals(new long[]{123L, 124L}, actualLong);
                },
                () -> {
                    Long[] actualLong = nodeProperties.propertyValue(
                            "longus-namus", new Long[]{DefaultProperties.LONG_ZERO}
                    );
                    assertArrayEquals(new Long[]{123L, 124L}, actualLong);
                },
                () -> {
                    double[] actualDouble = nodeProperties.propertyValue(
                            "doubleus-namus", new double[]{DefaultProperties.DOUBLE_ZERO}
                    );
                    assertArrayEquals(new double[]{99.99, 199.99}, actualDouble);
                },
                () -> {
                    Double[] actualDouble = nodeProperties.propertyValue(
                            "doubleus-namus", new Double[]{DefaultProperties.DOUBLE_ZERO}
                    );
                    assertArrayEquals(new Double[]{99.99, 199.99}, actualDouble);
                },
                () -> {
                    BigDecimal[] actualDecimal = nodeProperties.propertyValue(
                            "decimalus-namus", new BigDecimal[]{DefaultProperties.DECIMAL_ZERO}
                    );
                    assertArrayEquals(new BigDecimal[]{
                            new BigDecimal("999.99"), new BigDecimal("1999.99")
                    }, actualDecimal);
                },
                () -> {
                    Calendar[] actualCalendar = nodeProperties.propertyValue(
                            "calendarus-namus", new Calendar[]{DefaultProperties.DATE_UNIX_EPOCH}
                    );
                    assertArrayEquals(new Calendar[]{unix1980, unix1990}, actualCalendar);
                }
        );
    }

    @Test
    void mustGetBinarySize() {
        TargetJCRPath realAssetPath = new TargetJCRPath(
                new ParentJCRPath(new TargetJCRPath("/content")), UUID.randomUUID()
        );
        new StagedAssetReal(() -> Optional.of(file), new FileMetadata(file), resourceAccess).save(realAssetPath);
        TargetJCRPath ntFilePath = new TargetJCRPath(new ParentJCRPath(realAssetPath), Asset.FILE_NODE_NAME);
        TargetJCRPath ntResourcePath = new TargetJCRPath(new ParentJCRPath(ntFilePath), JcrConstants.JCR_CONTENT);
        NodeProperties ntFileNodeProperties = new NodeProperties(ntFilePath, resourceAccess);
        NodeProperties ntResourceNodeProperties = new NodeProperties(ntResourcePath, resourceAccess);
        assertAll(
                () -> assertEquals(
                        new DataSize(609_994, DataUnit.BYTES),
                        ntResourceNodeProperties.binarySize(JcrConstants.JCR_DATA)
                ),
                () -> assertEquals(
                        new DataSize(0, DataUnit.BYTES),
                        ntFileNodeProperties.binarySize(JcrConstants.JCR_DATA)
                )
        );
    }

    @Test
    void mustGetDefaultUnaryValues() {
        context.build().resource("/content").commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        assertAll(
                () -> {
                    String actualString = nodeProperties.propertyValue(
                            "stringus-namus", DefaultProperties.of("stringus-valus")
                    );
                    assertEquals("stringus-valus", actualString);
                },
                () -> {
                    @SuppressWarnings("ConstantValue")
                    boolean actualBoolean = nodeProperties.propertyValue(
                            "booleanus-namus", DefaultProperties.of(true)
                    );
                    assertTrue(actualBoolean);
                },
                () -> {
                    long actualLong = nodeProperties.propertyValue(
                            "longus-namus", DefaultProperties.of(123L)
                    );
                    assertEquals(123L, actualLong);
                },
                () -> {
                    double actualDouble = nodeProperties.propertyValue(
                            "doubleus-namus", DefaultProperties.of(99.99)
                    );
                    assertEquals(99.99, actualDouble);
                },
                () -> {
                    BigDecimal actualDecimal = nodeProperties.propertyValue(
                            "decimalus-namus", DefaultProperties.of(new BigDecimal("999.99"))
                    );
                    assertEquals(new BigDecimal("999.99"), actualDecimal);
                },
                () -> {
                    Calendar actualCalendar = nodeProperties.propertyValue(
                            "calendarus-namus", DefaultProperties.of(unix1980)
                    );
                    assertEquals(unix1980, actualCalendar);
                }
        );
    }

    @Test
    void mustGetFromClassUnaryExistingValues() {
        context.build().resource("/content",
                Map.of(
                        "stringus-namus", "stringus-valus",
                        "booleanus-namus", true,
                        "longus-namus", 123L,
                        "doubleus-namus", 99.99,
                        "decimalus-namus", new BigDecimal("999.99"),
                        "calendarus-namus", unix1980
                )
        ).commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        assertAll(
                () -> {
                    String actualString = nodeProperties.propertyValue(
                            "stringus-namus", DefaultProperties.STRING_CLASS
                    ).orElseThrow();
                    assertEquals("stringus-valus", actualString);
                },
                () -> {
                    boolean actualBoolean = nodeProperties.propertyValue(
                            "booleanus-namus", DefaultProperties.BOOLEAN_CLASS
                    ).orElseThrow();
                    assertTrue(actualBoolean);
                },
                () -> {
                    Boolean actualBoolean = nodeProperties.propertyValue(
                            "booleanus-namus", Boolean.class
                    ).orElseThrow();
                    assertTrue(actualBoolean);
                },
                () -> {
                    long actualLong = nodeProperties.propertyValue(
                            "longus-namus", DefaultProperties.LONG_CLASS
                    ).orElseThrow();
                    assertEquals(123L, actualLong);
                },
                () -> {
                    Long actualLong = nodeProperties.propertyValue(
                            "longus-namus", Long.class
                    ).orElseThrow();
                    assertEquals(123L, actualLong);
                },
                () -> {
                    double actualDouble = nodeProperties.propertyValue(
                            "doubleus-namus", DefaultProperties.DOUBLE_CLASS
                    ).orElseThrow();
                    assertEquals(99.99, actualDouble);
                },
                () -> {
                    Double actualDouble = nodeProperties.propertyValue(
                            "doubleus-namus", Double.class
                    ).orElseThrow();
                    assertEquals(99.99, actualDouble);
                },
                () -> {
                    BigDecimal actualDecimal = nodeProperties.propertyValue(
                            "decimalus-namus", DefaultProperties.DECIMAL_CLASS
                    ).orElseThrow();
                    assertEquals(new BigDecimal("999.99"), actualDecimal);
                },
                () -> {
                    Calendar actualCalendar = nodeProperties.propertyValue(
                            "calendarus-namus", DefaultProperties.DATE_CLASS
                    ).orElseThrow();
                    assertEquals(unix1980, actualCalendar);
                }
        );
    }

    @Test
    void mustGetFromClassUnaryNonExistingValues() {
        context.build().resource("/content").commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        assertAll(
                () -> {
                    boolean isEmpty = nodeProperties.propertyValue(
                            "stringus-namus", DefaultProperties.STRING_CLASS
                    ).isEmpty();
                    assertTrue(isEmpty);
                },
                () -> {
                    boolean isEmpty = nodeProperties.propertyValue(
                            "booleanus-namus", DefaultProperties.BOOLEAN_CLASS
                    ).isEmpty();
                    assertTrue(isEmpty);
                },
                () -> {
                    boolean isEmpty = nodeProperties.propertyValue(
                            "booleanus-namus", Boolean.class
                    ).isEmpty();
                    assertTrue(isEmpty);
                },
                () -> {
                    boolean isEmpty = nodeProperties.propertyValue(
                            "longus-namus", DefaultProperties.LONG_CLASS
                    ).isEmpty();
                    assertTrue(isEmpty);
                },
                () -> {
                    boolean isEmpty = nodeProperties.propertyValue(
                            "longus-namus", Long.class
                    ).isEmpty();
                    assertTrue(isEmpty);
                },
                () -> {
                    boolean isEmpty = nodeProperties.propertyValue(
                            "doubleus-namus", DefaultProperties.DOUBLE_CLASS
                    ).isEmpty();
                    assertTrue(isEmpty);
                },
                () -> {
                    boolean isEmpty = nodeProperties.propertyValue(
                            "doubleus-namus", Double.class
                    ).isEmpty();
                    assertTrue(isEmpty);
                },
                () -> {
                    boolean isEmpty = nodeProperties.propertyValue(
                            "decimalus-namus", DefaultProperties.DECIMAL_CLASS
                    ).isEmpty();
                    assertTrue(isEmpty);
                },
                () -> {
                    boolean isEmpty = nodeProperties.propertyValue(
                            "calendarus-namus", DefaultProperties.DATE_CLASS
                    ).isEmpty();
                    assertTrue(isEmpty);
                }
        );
    }

    @Test
    void mustGetFromClassMultiExistingValues() {
        context.build().resource("/content",
                Map.of(
                        "stringus-namus", new String[]{"stringus-valus-1", "stringus-valus-2"},
                        "booleanus-namus", new boolean[]{true, true},
                        "longus-namus", new long[]{123L, 124L},
                        "doubleus-namus", new double[]{99.99, 199.99},
                        "decimalus-namus", new BigDecimal[]{
                                new BigDecimal("999.99"), new BigDecimal("1999.99")
                        },
                        "calendarus-namus", new Calendar[]{unix1980, unix1990}
                )
        ).commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        assertAll(
                () -> {
                    String[] actualString = nodeProperties.propertyValue(
                            "stringus-namus", DefaultProperties.STRING_CLASS_ARRAY
                    ).orElseThrow();
                    assertArrayEquals(new String[]{"stringus-valus-1", "stringus-valus-2"}, actualString);
                },
                () -> {
                    boolean[] actualBoolean = nodeProperties.propertyValue(
                            "booleanus-namus", DefaultProperties.BOOLEAN_CLASS_ARRAY
                    ).orElseThrow();
                    assertArrayEquals(new boolean[]{true, true}, actualBoolean);
                },
                () -> {
                    Boolean[] actualBoolean = nodeProperties.propertyValue(
                            "booleanus-namus", Boolean[].class
                    ).orElseThrow();
                    assertArrayEquals(new Boolean[]{true, true}, actualBoolean);
                },
                () -> {
                    long[] actualLong = nodeProperties.propertyValue(
                            "longus-namus", DefaultProperties.LONG_CLASS_ARRAY
                    ).orElseThrow();
                    assertArrayEquals(new long[]{123L, 124L}, actualLong);
                },
                () -> {
                    Long[] actualLong = nodeProperties.propertyValue(
                            "longus-namus", Long[].class
                    ).orElseThrow();
                    assertArrayEquals(new Long[]{123L, 124L}, actualLong);
                },
                () -> {
                    double[] actualDouble = nodeProperties.propertyValue(
                            "doubleus-namus", DefaultProperties.DOUBLE_CLASS_ARRAY
                    ).orElseThrow();
                    assertArrayEquals(new double[]{99.99, 199.99}, actualDouble);
                },
                () -> {
                    Double[] actualDouble = nodeProperties.propertyValue(
                            "doubleus-namus", Double[].class
                    ).orElseThrow();
                    assertArrayEquals(new Double[]{99.99, 199.99}, actualDouble);
                },
                () -> {
                    BigDecimal[] actualDecimal = nodeProperties.propertyValue(
                            "decimalus-namus", DefaultProperties.DECIMAL_CLASS_ARRAY
                    ).orElseThrow();
                    assertArrayEquals(new BigDecimal[]{
                            new BigDecimal("999.99"), new BigDecimal("1999.99")
                    }, actualDecimal);
                },
                () -> {
                    Calendar[] actualCalendar = nodeProperties.propertyValue(
                            "calendarus-namus", DefaultProperties.DATE_CLASS_ARRAY
                    ).orElseThrow();
                    assertArrayEquals(new Calendar[]{unix1980, unix1990}, actualCalendar);
                }
        );
    }

    @Test
    void mustCheckPropertyExistence() {
        context.build().resource("/content",
                Map.of(
                        "stringus-namus", "stringus-valus",
                        "booleanus-namus", true,
                        "longus-namus", 123L,
                        "doubleus-namus", 99.99,
                        "decimalus-namus", new BigDecimal("999.99"),
                        "calendarus-namus", unix1980
                )
        ).commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        assertAll(
                () -> assertTrue(nodeProperties.containsProperty("stringus-namus")),
                () -> assertFalse(nodeProperties.containsProperty("non-existent"))
        );
    }

    @Test
    void mustThrowForBadType() {
        context.build().resource("/content").commit();
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath("/content"), resourceAccess);
        String primaryType = nodeProperties.primaryType();
        assertAll(
                () -> assertEquals(JcrConstants.NT_UNSTRUCTURED, primaryType),
                () -> assertThrows(
                        IllegalPrimaryTypeException.class,
                        () -> nodeProperties.assertPrimaryType("invalid-prim-type")
                )
        );
    }
}
