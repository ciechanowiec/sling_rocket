package eu.ciechanowiec.sling.rocket.observation.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import lombok.SneakyThrows;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Piece of information about an event that occurred in the application and is relevant for auditing purposes.
 */
@Model(adaptables = Resource.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class Entry implements Comparable<Entry>, JSON {

    /**
     * The type name of a {@link Node} that represents an {@link Entry} in the {@link Repository}.
     */
    @SuppressWarnings({"WeakerAccess", "StaticMethodOnlyUsedInOneClass"})
    public static final String NT_AUDIT_ENTRY = "rocket:AuditEntry";

    /**
     * Name of a {@link Property} of type {@link PropertyType#STRING} that holds the ID of the user related to the event
     * described by this {@link Entry}.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String PN_USER_ID = "userID";

    /**
     * Name of a {@link Property} of type {@link PropertyType#STRING} that holds the target of the event described by
     * this {@link Entry}, e.g., a {@link JCRPath}.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String PN_SUBJECT = "subject";

    /**
     * Name of a {@link Property} of type {@link PropertyType#LONG} that holds the time when the event described by this
     * {@link Entry} occurred, represented as the number of seconds since the UNIX epoch.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String PN_TIMESTAMP = "timestamp";

    /**
     * Value used as a default for {@link Property}-s of {@link Node}-s of type {@link Entry#NT_AUDIT_ENTRY} when the
     * actual value is unknown, e.g., when an {@link Entry} is created based on a {@link ResourceChange} that does not
     * provide the ID of the user related to the event described by this {@link Entry}.
     */
    public static final String UNKNOWN = "unknown";

    static final Set<String> SYSTEM_PROPERTIES = Set.of(
        JcrConstants.JCR_CREATED,
        JcrConstants.JCR_CREATED_BY,
        JcrConstants.JCR_LASTMODIFIED,
        JcrConstants.JCR_LAST_MODIFIED_BY,
        JcrConstants.JCR_PRIMARYTYPE
    );
    static final String PN_ADDITIONAL_PROPERTIES = "additionalProperties";

    private final Supplier<String> userID;
    private final Supplier<String> subject;
    private final Supplier<LocalDateTime> timestamp;
    private final Supplier<Map<String, String>> additionalProperties;
    @SuppressWarnings("OverlyComplexBooleanExpression")
    private final Predicate<Map.Entry<String, String>> additionalPropertiesFilter = entry -> {
        String key = entry.getKey();
        return !SYSTEM_PROPERTIES.contains(key)
            && !PN_USER_ID.equals(key)
            && !PN_SUBJECT.equals(key)
            && !PN_TIMESTAMP.equals(key);
    };

    /**
     * Constructs an instance of this class.
     *
     * @param userID               ID of the user related to the event described by this {@link Entry}
     * @param subject              target of the event described by this {@link Entry}, e.g., {@link JCRPath}
     * @param timestamp            time when the event described by this {@link Entry} occurred
     * @param additionalProperties additional properties related to the event described by this {@link Entry}
     */
    @SuppressWarnings("WeakerAccess")
    public Entry(String userID, String subject, LocalDateTime timestamp, Map<String, String> additionalProperties) {
        this.userID = () -> userID;
        this.subject = () -> subject;
        this.timestamp = () -> timestamp;
        this.additionalProperties = () -> additionalProperties.entrySet()
            .stream()
            .filter(additionalPropertiesFilter)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Constructs an instance of this class.
     *
     * @param userID    ID of the user related to the event described by this {@link Entry}
     * @param subject   target of the event described by this {@link Entry}, e.g., {@link JCRPath}
     * @param timestamp time when the event described by this {@link Entry} occurred
     * @param resource  {@link Resource} that represents a {@link Node} of type {@link Entry#NT_AUDIT_ENTRY}
     */
    @Inject
    public Entry(
        @ValueMapValue(name = PN_USER_ID)
        @Default(values = UNKNOWN)
        String userID,
        @ValueMapValue(name = PN_SUBJECT)
        @Default(values = UNKNOWN)
        String subject,
        @ValueMapValue(name = PN_TIMESTAMP)
        @Default(longValues = 0L)
        long timestamp,
        @Self
        Resource resource
    ) {
        this.userID = () -> userID;
        this.subject = () -> subject;
        this.timestamp = () -> LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        NodeProperties nodeProperties = new NodeProperties(resource);
        Map<String, String> extractedAdditionalProperties = nodeProperties.all()
            .entrySet()
            .stream()
            .filter(additionalPropertiesFilter)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        this.additionalProperties = () -> extractedAdditionalProperties;
    }

    /**
     * Constructs an instance of this class.
     *
     * @param resourceChange {@link ResourceChange} that should be registered as an {@link Entry}
     */
    public Entry(ResourceChange resourceChange) {
        this.userID = () -> Optional.ofNullable(resourceChange.getUserId()).orElse(UNKNOWN);
        this.subject = resourceChange::getPath;
        this.timestamp = LocalDateTime::now;
        this.additionalProperties = () -> Map.of(
            "changeType", resourceChange.getType().name(),
            "source", ResourceChange.class.getName()
        );
    }

    /**
     * ID of the user related to the event described by this {@link Entry}.
     *
     * @return ID of the user related to the event described by this {@link Entry}
     */
    @SuppressWarnings("WeakerAccess")
    @JsonProperty
    public String userID() {
        return userID.get();
    }

    /**
     * Target of the event described by this {@link Entry}, e.g., a {@link JCRPath}.
     *
     * @return target of the event described by this {@link Entry}, e.g., a {@link JCRPath}
     */
    @SuppressWarnings("WeakerAccess")
    @JsonProperty
    public String subject() {
        return subject.get();
    }

    /**
     * Time when the event described by this {@link Entry} occurred.
     *
     * @return time when the event described by this {@link Entry} occurred
     */
    @SuppressWarnings("WeakerAccess")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    @JsonProperty
    public LocalDateTime timestamp() {
        return timestamp.get();
    }

    /**
     * Additional properties related to the event described by this {@link Entry}.
     *
     * @return additional properties related to the event described by this {@link Entry}
     */
    @SuppressWarnings("WeakerAccess")
    @JsonProperty
    public Map<String, String> additionalProperties() {
        return additionalProperties.get();
    }

    Map<String, Object> asSlingJobProperties() {
        Map<String, Object> combinedProps = new ConcurrentHashMap<>();
        combinedProps.put(PN_USER_ID, userID());
        combinedProps.put(PN_SUBJECT, subject());
        combinedProps.put(PN_TIMESTAMP, timestamp().atZone(ZoneId.systemDefault()).toEpochSecond());
        combinedProps.put(PN_ADDITIONAL_PROPERTIES, additionalProperties());
        return Collections.unmodifiableMap(combinedProps);
    }

    @Override
    public String toString() {
        return asJSON();
    }

    @Override
    public int compareTo(Entry comparedEntry) {
        return timestamp().compareTo(comparedEntry.timestamp());
    }

    @Override
    public boolean equals(Object comparedObject) {
        return this == comparedObject
            || comparedObject instanceof Entry comparedEntry
            && userID().equals(comparedEntry.userID())
            && subject().equals(comparedEntry.subject())
            && timestamp().equals(comparedEntry.timestamp())
            && additionalProperties().equals(comparedEntry.additionalProperties());
    }

    @Override
    public int hashCode() {
        int result = userID().hashCode();
        result = 31 * result + subject().hashCode();
        result = 31 * result + timestamp().hashCode();
        result = 31 * result + additionalProperties().hashCode();
        return result;
    }

    @Override
    @SneakyThrows
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return objectMapper.writeValueAsString(this);
    }
}
