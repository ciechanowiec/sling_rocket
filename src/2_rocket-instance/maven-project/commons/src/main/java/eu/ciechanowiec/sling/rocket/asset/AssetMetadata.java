package eu.ciechanowiec.sling.rocket.asset;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import javax.jcr.Node;
import javax.jcr.Repository;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents metadata of an {@link Asset}, either actually persisted and existing in the {@link Repository}
 * or a hypothetical one. Might be of two types:
 * <ol>
 *     <li>Backed by a persistent {@link Node} of type {@link Asset#NT_ASSET_METADATA}.</li>
 *     <li>Backed by an in-memory data structure.</li>
 * </ol>
 */
public interface AssetMetadata {

    /**
     * Name of the mime type property of the associated {@link Asset}.
     */
    String PN_MIME_TYPE = "mimeType";

    /**
     * Returns the mime type of the associated {@link Asset}.
     * @return mime type of the associated {@link Asset}
     */
    String mimeType();

    /**
     * Returns all properties of the associated {@link Asset} as a {@link Map}
     * of property names to property values converted to {@link String}; if a
     * given value cannot be converted to {@link String}, it is omitted from the result.
     * @return all properties of the associated {@link Asset} as a {@link Map}
     *         of property names to property values converted to {@link String}; if a
     *         given value cannot be converted to {@link String}, it is omitted from the result
     */
    Map<String, String> all();

    /**
     * Returns a {@link Map} of all properties returned by {@link AssetMetadata#all()},
     * where all values are cast to {@link Object}-s.
     * @return {@link Map} of all properties returned by {@link AssetMetadata#all()}, where
     *         all values are cast to {@link Object}-s
     */
    @SuppressWarnings("unchecked")
    default Map<String, Object> allButObjectValues() {
        return (Map<String, Object>) (Map<?, ?>) all();
    }

    /**
     * Returns an {@link Optional} containing the filename extension of the associated {@link Asset}
     * and prepended with a dot, e.g. {@code '.txt'}, {@code '.jpg'}, {@code '.pdf'};
     * if the filename extension cannot be determined, an empty {@link Optional} is returned.
     * @return {@link Optional} containing the filename extension of the associated {@link Asset}
     *         and prepended with a dot, e.g. {@code '.txt'}, {@code '.jpg'}, {@code '.pdf'};
     *         if the filename extension cannot be determined, an empty {@link Optional} is returned
     */
    @SuppressWarnings({"unchecked", "squid:S1612", "squid:S1166", "PMD.LambdaCanBeMethodReference"})
    default Optional<String> filenameExtension() {
        MimeTypes defaultMimeTypes = MimeTypes.getDefaultMimeTypes();
        try {
            MimeType mimeType = defaultMimeTypes.forName(mimeType());
            String extension = mimeType.getExtension();
            boolean isInvalidExtension = extension.isBlank();
            return Conditional.conditional(isInvalidExtension)
                    .onTrue(() -> Optional.empty())
                    .onFalse(() -> Optional.of(extension))
                    .get(Optional.class);
        } catch (MimeTypeException exception) {
            return Optional.empty();
        }
    }

    /**
     * Creates a new instance of {@link AssetMetadata} that has a property of the
     * associated {@link Asset} specified by the passed parameters set.
     * @param key name of a property of the associated {@link Asset}
     * @param value value of a new property of the associated {@link Asset}
     * @return new instance of {@link AssetMetadata} that has a property of the
     *         associated {@link Asset} specified by the passed parameters set
     */
    default AssetMetadata set(String key, String value) {
        CompletableFuture<Map<String, String>> newValuesSupplier = CompletableFuture.supplyAsync(
                () -> {
                    Map<String, String> newValues = new ConcurrentHashMap<>(all());
                    newValues.put(key, value);
                    return Collections.unmodifiableMap(newValues);
                }
        );
        CompletableFuture<String> mimeTypeSupplier = CompletableFuture.supplyAsync(this::mimeType);
        CompletableFuture<Optional<NodeProperties>> properties = CompletableFuture.supplyAsync(
                () -> properties().flatMap(
                        nodeProperties -> nodeProperties.setProperty(key, value)
                )
        );
        return new AssetMetadata() {
            @Override
            public String mimeType() {
                return mimeTypeSupplier.join();
            }

            @Override
            public Map<String, String> all() {
                return properties().map(NodeProperties::all).orElseGet(newValuesSupplier::join);
            }

            @Override
            public Optional<NodeProperties> properties() {
                return properties.join();
            }
        };
    }

    /**
     * If this {@link AssetMetadata} is backed by a {@link Node} of type {@link Asset#NT_ASSET_METADATA},
     * returns an {@link Optional} containing {@link NodeProperties} of that {@link Node};
     * otherwise, an empty {@link Optional} is returned.
     * @return if this {@link AssetMetadata} is backed by a {@link Node} of type {@link Asset#NT_ASSET_METADATA},
     *         returns an {@link Optional} containing {@link NodeProperties} of that {@link Node};
     *         otherwise, an empty {@link Optional} is returned
     */
    Optional<NodeProperties> properties();
}
