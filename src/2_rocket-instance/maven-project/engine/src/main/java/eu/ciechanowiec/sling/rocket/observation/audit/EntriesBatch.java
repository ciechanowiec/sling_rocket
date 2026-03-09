package eu.ciechanowiec.sling.rocket.observation.audit;

import com.google.common.primitives.Longs;
import org.apache.sling.event.jobs.Job;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class EntriesBatch {

    static final String PN_ENTRIES_BATCHED = "entriesBatched";
    private final Supplier<List<Entry>> entriesSupplier;

    EntriesBatch(Job job) {
        this.entriesSupplier = () -> Optional.ofNullable((List<?>) job.getProperty(PN_ENTRIES_BATCHED, List.class))
            .orElse(List.of())
            .stream()
            .filter(Map.class::isInstance)
            .map(object -> (Map<?, ?>) object)
            .map(this::onlyWithStringKeys)
            .map(this::toEntry)
            .toList();
    }

    EntriesBatch(List<Entry> entries) {
        this.entriesSupplier = () -> Collections.unmodifiableList(entries);
    }

    private Map<String, Object> onlyWithStringKeys(Map<?, ?> map) {
        return map.entrySet()
            .stream()
            .filter(entry -> entry.getKey() instanceof String)
            .collect(
                Collectors.toMap(
                    entry -> (String) entry.getKey(),
                    Map.Entry::getValue
                )
            );
    }

    @SuppressWarnings({"MethodWithMoreThanThreeNegations", "UnstableApiUsage"})
    private Entry toEntry(Map<String, Object> entryPropertiesFromSlingJob) {
        String userID = (String) entryPropertiesFromSlingJob.getOrDefault(Entry.PN_USER_ID, Entry.UNKNOWN);
        String subject = (String) entryPropertiesFromSlingJob.getOrDefault(Entry.PN_SUBJECT, Entry.UNKNOWN);
        LocalDateTime timestamp = Optional.ofNullable(entryPropertiesFromSlingJob.get(Entry.PN_TIMESTAMP))
            .map(
                timestampObj -> timestampObj instanceof Long timestampLong ?
                    timestampLong : Longs.tryParse(String.valueOf(timestampObj))
            ).map(
                unixTimeStamp -> {
                    Instant instant = Instant.ofEpochSecond(unixTimeStamp);
                    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                }
            ).orElse(LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC));
        Map<String, String> additionalProperties = Optional.ofNullable(
                entryPropertiesFromSlingJob.get(Entry.PN_ADDITIONAL_PROPERTIES)
            ).filter(Map.class::isInstance)
            .map(object -> (Map<?, ?>) object)
            .orElse(Map.of())
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)
            .filter(entry -> !Entry.SYSTEM_PROPERTIES.contains(entry.getKey()))
            .filter(entry -> {
                    String key = (String) entry.getKey();
                    return !Entry.PN_USER_ID.equals(key) && !Entry.PN_SUBJECT.equals(key)
                        && !Entry.PN_TIMESTAMP.equals(key);
                }
            ).collect(
                Collectors.toMap(
                    entry -> (String) entry.getKey(),
                    entry -> (String) entry.getValue()
                )
            );
        return new Entry(userID, subject, timestamp, additionalProperties);
    }

    List<Map<String, Object>> asSlingJobProperties() {
        return entriesSupplier.get().stream()
            .map(Entry::asSlingJobProperties)
            .toList();
    }

    List<Entry> entries() {
        return entriesSupplier.get();
    }
}
