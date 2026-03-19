package eu.ciechanowiec.sling.rocket.observation.audit;

import org.apache.jackrabbit.vault.util.JcrConstants;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings(
    {
        "EqualsWithItself", "MagicNumber", "ClassWithTooManyMethods", "MethodCount", "MultipleStringLiterals",
        "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"
    }
)
class EntryTest {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 3, 18, 12, 0, 0);

    private Entry entry(String userID, String subject, LocalDateTime timestamp, Map<String, String> props) {
        return new Entry(userID, subject, timestamp, props);
    }

    // ─── toString ────────────────────────────────────────────────────────────

    @Test
    void testToStringContainsFields() {
        Entry entry = entry("alice", "/content/page", TIMESTAMP, Map.of("key", "value"));
        String json = entry.toString();
        assertAll(
            () -> assertTrue(json.contains("alice"), "should contain userID"),
            () -> assertTrue(json.contains("/content/page"), "should contain subject"),
            () -> assertTrue(json.contains("2026-03-18"), "should contain formatted date"),
            () -> assertTrue(json.contains("key"), "should contain additional prop key"),
            () -> assertTrue(json.contains("value"), "should contain additional prop value")
        );
    }

    @Test
    void testToStringIsValidJson() {
        Entry entry = entry("bob", "/home", TIMESTAMP, Map.of());
        String json = entry.toString();
        assertTrue(json.startsWith("{") && json.endsWith("}"), "toString should produce a JSON object");
    }

    @Test
    void testToStringWithNoAdditionalProperties() {
        Entry entry = entry("carol", "/apps", TIMESTAMP, Map.of());
        String json = entry.toString();
        assertTrue(json.contains("carol"));
        assertTrue(json.contains("/apps"));
    }

    // ─── compareTo ───────────────────────────────────────────────────────────

    @Test
    void testCompareToEqualTimestamps() {
        Entry entry1 = entry("u1", "s1", TIMESTAMP, Map.of());
        Entry entry2 = entry("u2", "s2", TIMESTAMP, Map.of());
        assertEquals(0, entry1.compareTo(entry2), "entries with identical timestamps should compare as equal");
    }

    @Test
    void testCompareToEarlierTimestamp() {
        Entry earlier = entry("u1", "s1", TIMESTAMP.minusDays(1), Map.of());
        Entry later = entry("u2", "s2", TIMESTAMP, Map.of());
        assertTrue(earlier.compareTo(later) < 0, "earlier entry should be less than later entry");
    }

    @Test
    void testCompareToLaterTimestamp() {
        Entry earlier = entry("u1", "s1", TIMESTAMP, Map.of());
        Entry later = entry("u2", "s2", TIMESTAMP.plusHours(3), Map.of());
        assertTrue(later.compareTo(earlier) > 0, "later entry should be greater than earlier entry");
    }

    @Test
    void testCompareToIsSelfConsistent() {
        Entry entry = entry("u", "s", TIMESTAMP, Map.of());
        assertEquals(0, entry.compareTo(entry), "an entry compared to itself should return 0");
    }

    @Test
    void testCompareToAntiSymmetry() {
        Entry entry1 = entry("u1", "s1", TIMESTAMP.minusMinutes(30), Map.of());
        Entry entry2 = entry("u2", "s2", TIMESTAMP, Map.of());
        int comparison1to2 = entry1.compareTo(entry2);
        int comparison2to1 = entry2.compareTo(entry1);
        assertTrue(comparison1to2 < 0 && comparison2to1 > 0, "compareTo must be antisymmetric");
    }

    // ─── equals ──────────────────────────────────────────────────────────────

    @Test
    void testEqualsReflexivity() {
        Entry entry = entry("alice", "/content", TIMESTAMP, Map.of("a", "b"));
        assertEquals(entry, entry);
    }

    @Test
    void testEqualsSymmetry() {
        Entry entry1 = entry("alice", "/content", TIMESTAMP, Map.of("a", "b"));
        Entry entry2 = entry("alice", "/content", TIMESTAMP, Map.of("a", "b"));
        assertEquals(entry1, entry2);
        assertEquals(entry2, entry1);
    }

    @Test
    void testEqualsTransitivity() {
        Entry entry1 = entry("alice", "/content", TIMESTAMP, Map.of("k", "v"));
        Entry entry2 = entry("alice", "/content", TIMESTAMP, Map.of("k", "v"));
        Entry entry3 = entry("alice", "/content", TIMESTAMP, Map.of("k", "v"));
        assertEquals(entry1, entry2);
        assertEquals(entry2, entry3);
        assertEquals(entry1, entry3);
    }

    @Test
    void testEqualsNotEqualWhenUserIDDiffers() {
        Entry entry1 = entry("alice", "/content", TIMESTAMP, Map.of());
        Entry entry2 = entry("bob", "/content", TIMESTAMP, Map.of());
        assertNotEquals(entry1, entry2);
    }

    @Test
    void testEqualsNotEqualWhenSubjectDiffers() {
        Entry entry1 = entry("alice", "/content/a", TIMESTAMP, Map.of());
        Entry entry2 = entry("alice", "/content/b", TIMESTAMP, Map.of());
        assertNotEquals(entry1, entry2);
    }

    @Test
    void testEqualsNotEqualWhenTimestampDiffers() {
        Entry entry1 = entry("alice", "/content", TIMESTAMP, Map.of());
        Entry entry2 = entry("alice", "/content", TIMESTAMP.plusSeconds(1), Map.of());
        assertNotEquals(entry1, entry2);
    }

    @Test
    void testEqualsNotEqualWhenAdditionalPropsDiffer() {
        Entry entry1 = entry("alice", "/content", TIMESTAMP, Map.of("k", "v1"));
        Entry entry2 = entry("alice", "/content", TIMESTAMP, Map.of("k", "v2"));
        assertNotEquals(entry1, entry2);
    }

    @Test
    void testEqualsNullReturnsFalse() {
        Entry entry = entry("alice", "/content", TIMESTAMP, Map.of());
        assertNotEquals(null, entry);
    }

    @Test
    void testEqualsOtherTypeReturnsFalse() {
        Entry entry = entry("alice", "/content", TIMESTAMP, Map.of());
        assertNotEquals(new Object(), entry);
    }

    @Test
    void testEqualsSystemPropertiesAreFilteredOut() {
        // jcr:created and friends must be stripped - two entries that differ only in system props must be equal
        Map<String, String> withSystem = new ConcurrentHashMap<>();
        withSystem.put("myKey", "myVal");
        withSystem.put(JcrConstants.JCR_CREATED, "2000-01-01");      // system property - will be filtered
        withSystem.put(JcrConstants.JCR_CREATED_BY, "system");       // system property - will be filtered

        Entry entry1 = entry("alice", "/content", TIMESTAMP, withSystem);
        Entry entry2 = entry("alice", "/content", TIMESTAMP, Map.of("myKey", "myVal"));
        assertEquals(entry1, entry2, "system properties must be ignored in equality");
    }

    // ─── hashCode ────────────────────────────────────────────────────────────

    @Test
    void testHashCodeConsistency() {
        Entry entry = entry("alice", "/content", TIMESTAMP, Map.of("k", "v"));
        assertEquals(entry.hashCode(), entry.hashCode(), "hashCode must be consistent across calls");
    }

    @Test
    void testHashCodeEqualObjectsHaveEqualHashCodes() {
        Entry entry1 = entry("alice", "/content", TIMESTAMP, Map.of("k", "v"));
        Entry entry2 = entry("alice", "/content", TIMESTAMP, Map.of("k", "v"));
        assertEquals(entry1, entry2);
        assertEquals(
            entry1.hashCode(), entry2.hashCode(),
            "equal objects must have the same hashCode"
        );
    }

    @Test
    void testHashCodeDifferentUserIDDiffers() {
        Entry entry1 = entry("alice", "/content", TIMESTAMP, Map.of());
        Entry entry2 = entry("bob", "/content", TIMESTAMP, Map.of());
        // Not guaranteed by contract, but a good sanity check for the quality of the hash function
        assertNotEquals(
            entry1.hashCode(), entry2.hashCode(),
            "entries with different userIDs should have different hash codes"
        );
    }

    @Test
    void testHashCodeDifferentTimestampDiffers() {
        Entry entry1 = entry("alice", "/content", TIMESTAMP, Map.of());
        Entry entry2 = entry("alice", "/content", TIMESTAMP.plusDays(1), Map.of());
        assertNotEquals(entry1.hashCode(), entry2.hashCode());
    }

    @Test
    void testHashCodeUsedInHashSet() {
        Entry entry1 = entry("alice", "/content", TIMESTAMP, Map.of("k", "v"));
        Entry entry2 = entry("alice", "/content", TIMESTAMP, Map.of("k", "v"));  // equal to entry1
        Entry entry3 = entry("bob", "/content", TIMESTAMP, Map.of());            // different

        Collection<Entry> set = new HashSet<>();
        set.add(entry1);
        set.add(entry2); // duplicate - should not increase size
        set.add(entry3);

        assertEquals(2, set.size(), "HashSet should treat equal entries as one element");
    }

    @Test
    void testEntriesBatchSorting() {
        LocalDateTime first = LocalDateTime.of(2026, 1, 1, 8, 0);
        LocalDateTime second = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime third = LocalDateTime.of(2026, 1, 1, 12, 0);

        // Provide entries intentionally out of chronological order
        List<Entry> unordered = List.of(
            new Entry("u3", "s3", third, Map.of()),
            new Entry("u1", "s1", first, Map.of()),
            new Entry("u2", "s2", second, Map.of())
        );

        EntriesBatch batch = new EntriesBatch(unordered);
        List<Entry> sorted = batch.entries();

        assertAll(
            () -> assertEquals(3, sorted.size()),
            () -> assertEquals(first, sorted.getFirst().timestamp()),
            () -> assertEquals(second, sorted.get(1).timestamp()),
            () -> assertEquals(third, sorted.get(2).timestamp())
        );
    }
}
