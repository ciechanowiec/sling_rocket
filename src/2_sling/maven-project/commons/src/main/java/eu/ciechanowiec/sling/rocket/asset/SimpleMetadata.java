package eu.ciechanowiec.sling.rocket.asset;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple metadata of a media file.
 */
public interface SimpleMetadata {

    /**
     * Name of the mime type property of the associated media file.
     */
    String PN_MIME_TYPE = "mimeType";

    /**
     * Returns the mime type of the associated media file.
     * @return mime type of the associated media file
     */
    String mimeType();

    /**
     * Returns a map of all properties of the associated media file.
     * @return map of all properties of the associated media file,
     * including the mime type returned by {@link SimpleMetadata#mimeType()}
     */
    Map<String, String> all();

    /**
     * Returns a map of all properties returned by {@link SimpleMetadata#all()},
     * where all values are cast to {@link Object}-s.
     * @return map of all properties returned by {@link SimpleMetadata#all()}, where
     *         all values are cast to {@link Object}-s
     */
    @SuppressWarnings("unchecked")
    default Map<String, Object> allButObjectValues() {
        return (Map<String, Object>) (Map<?, ?>) all();
    }

    /**
     * Creates a new instance of {@link SimpleMetadata} that additionally contains
     * a property of a media file specified by the passed parameters.
     * @param key name of a new property of a media file
     * @param value value of a new property of a media file
     * @return new instance of {@link SimpleMetadata} that additionally contains
     *         a property of a media file specified by the passed parameters
     */
    default SimpleMetadata append(String key, String value) {
        Map<String, String> newValues = new ConcurrentHashMap<>(all());
        newValues.put(key, value);
        String mimeType = mimeType();
        return new SimpleMetadata() {
            @Override
            public String mimeType() {
                return mimeType;
            }

            @Override
            public Map<String, String> all() {
                return Collections.unmodifiableMap(newValues);
            }
        };
    }
}
