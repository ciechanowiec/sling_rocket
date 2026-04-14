package eu.ciechanowiec.sling.rocket.observation.stats.consistency;

import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.observation.audit.EntryTrampoline;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JCRConsistencyStatsTest extends TestEnvironment {

    private EntryTrampoline entryTrampoline;

    @TempDir
    private Path tempDir;

    JCRConsistencyStatsTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        entryTrampoline = context.registerService(EntryTrampoline.class, mock(EntryTrampoline.class));
    }

    @Test
    void testName() {
        JCRConsistencyStats stats = context.registerInjectActivateService(JCRConsistencyStats.class);
        assertEquals(JCRConsistencyStats.class.getName(), stats.name());
    }

    @SneakyThrows
    @Test
    void testAsJSONAvailableNoJournal() {
        Path segmentStore = tempDir.resolve("segmentstore_no_journal").toAbsolutePath();
        Files.createDirectories(segmentStore);
        JSON stats = context.registerInjectActivateService(
            JCRConsistencyStats.class, Map.of("backup.segmentstore.path", segmentStore.toString())
        );
        String json = stats.asJSON();
        assertTrue(json.contains("\"backupSegmentStore\":{"));
        assertTrue(json.contains("\"journalLogRecentSegments\":[]"));
    }

    @SneakyThrows
    @Test
    @SuppressWarnings(
        {"LineLength", "NestedTryDepth", "NestedTryStatement", "PMD.UseExplicitTypes", "resource", "MethodLength"}
    )
    void testRunCheck() {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.isDirectory(any())).thenAnswer(
                invocation -> {
                    String path = invocation.getArgument(0).toString();
                    return path.startsWith("/opt/sling") || path.contains("backup");
                }
            );
            mockedFiles.when(() -> Files.isRegularFile(any())).thenReturn(true);
            mockedFiles.when(() -> Files.exists(any())).thenReturn(true);
            // Mock journal.log for both stores
            mockedFiles.when(() -> Files.lines(any(), any())).thenAnswer(
                _ -> Stream.of(
                    "fc9daea1-2f9b-4515-af4a-e455620a122a:162 root 1775972380768",
                    "52e57995-a3b6-4a8c-ad18-0572ee4fc8ef:62 root 1775972385792",
                    "362623a0-b23d-4a3f-a76d-37dd75bb8043:161 root 1775972395808",
                    "74c53598-0f8b-4a75-a464-e0174c6e9969:62 root 1775972400820",
                    "dd9bdfb7-e70a-4596-a673-a58ad7ee21da:88 root 1775972481107",
                    "0a59b69a-7565-4f3c-a134-43c9c83ca593:861 root 1775972486474",
                    "e3d9232f-e613-4c82-a067-a5220e4d83b4:61 root 1775972491720",
                    "fd06b674-e505-45d7-a80e-27343048cf4a:61 root 1775972576978",
                    "904081c7-a80e-4916-a7ab-88afa7dcaa0f:61 root 1775972877578",
                    "c9e12bcc-8f07-4ee2-a1cd-d1aadc6688a8:61 root 1775973478934",
                    "5d4e56df-ff31-47a6-ae37-fe6482ccf2f1:61 root 1775974079908",
                    "355c9fa2-ae89-4fb9-acab-1b74e60da6cf:75 root 1775974676382",
                    "ca57d311-2ba7-4c2f-adc3-19bb9f7bf657:61 root 1775974681395"
                )
            );
            String output = """
                 root@rocket-instance:/opt/sling/launcher/repository/segmentstore# java -jar /opt/sling/oak-run-1.92.0.jar check /opt/sling/launcher/repository/segmentstore --bin
                 Apache Jackrabbit Oak 1.92.0
                 06:24:27.061 [segmentstore-init-4] WARN  o.a.j.o.s.file.tar.SegmentTarReader [loadAndValidateIndex:125] - Unable to load index of file data00003a.tar: Unrecognized magic number
                 06:24:27.064 [segmentstore-init-4] WARN  o.a.j.o.s.file.tar.SegmentTarReader [loadAndValidateIndex:125] - Unable to load index of file data00003a.tar: Unrecognized magic number

                 Checking revision 5d4e56df-ff31-47a6-ae37-fe6482ccf2f1:61

                 Checking head

                 Checking /
                 Checked 64,492 nodes and 176,791 properties
                 Path / is consistent

                 Checking checkpoints

                 Checking checkpoint c1dc30bd-fecd-4455-ac01-17d523f48ee2
                 Checking /
                 Checked 64,492 nodes and 176,791 properties
                 Path / is consistent

                 Searched through 1 revisions and 1 checkpoints

                 Head
                 Latest good revision for path / is 5d4e56df-ff31-47a6-ae37-fe6482ccf2f1:61 from Apr 12, 2026, 6:18:01 AM

                 Checkpoints
                 - c1dc30bd-fecd-4455-ac01-17d523f48ee2
                   Latest good revision for path / is 5d4e56df-ff31-47a6-ae37-fe6482ccf2f1:61 from Apr 12, 2026, 6:18:01 AM

                 Overall
                 Latest good revision for paths and checkpoints checked is 5d4e56df-ff31-47a6-ae37-fe6482ccf2f1:61 from Apr 12, 2026, 6:18:01 AM
                """;
            try (
                var _ = mockConstruction(
                    ProcessBuilder.class, (builder, _) -> {
                        Process process = mock(Process.class);
                        when(process.getInputStream()).thenReturn(
                            new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
                        when(process.waitFor()).thenReturn(0);
                        when(builder.start()).thenReturn(process);
                    }
                )
            ) {
                JSON stats = context.registerInjectActivateService(
                    JCRConsistencyStats.class, Map.of("backup.segmentstore.path", "/var/rocket-data-dump/backup")
                );
                String json = stats.asJSON();
                String expected
                    = "{\"backupSegmentStore\":{\"segmentStoreAbsPath\":\"/var/rocket-data-dump/backup\",\"lastValidSegment\":\"5d4e56df-ff31-47a6-ae37-fe6482ccf2f1:61\",\"journalLogRecentSegments\":[\"fc9daea1-2f9b-4515-af4a-e455620a122a:162\",\"52e57995-a3b6-4a8c-ad18-0572ee4fc8ef:62\",\"362623a0-b23d-4a3f-a76d-37dd75bb8043:161\",\"74c53598-0f8b-4a75-a464-e0174c6e9969:62\",\"dd9bdfb7-e70a-4596-a673-a58ad7ee21da:88\",\"0a59b69a-7565-4f3c-a134-43c9c83ca593:861\",\"e3d9232f-e613-4c82-a067-a5220e4d83b4:61\",\"fd06b674-e505-45d7-a80e-27343048cf4a:61\",\"904081c7-a80e-4916-a7ab-88afa7dcaa0f:61\",\"c9e12bcc-8f07-4ee2-a1cd-d1aadc6688a8:61\",\"5d4e56df-ff31-47a6-ae37-fe6482ccf2f1:61\",\"355c9fa2-ae89-4fb9-acab-1b74e60da6cf:75\",\"ca57d311-2ba7-4c2f-adc3-19bb9f7bf657:61\"],\"consistencyIssues\":false},\"nativeSegmentStore\":{\"segmentStoreAbsPath\":\"/opt/sling/launcher/repository/segmentstore\",\"lastValidSegment\":\"5d4e56df-ff31-47a6-ae37-fe6482ccf2f1:61\",\"journalLogRecentSegments\":[\"fc9daea1-2f9b-4515-af4a-e455620a122a:162\",\"52e57995-a3b6-4a8c-ad18-0572ee4fc8ef:62\",\"362623a0-b23d-4a3f-a76d-37dd75bb8043:161\",\"74c53598-0f8b-4a75-a464-e0174c6e9969:62\",\"dd9bdfb7-e70a-4596-a673-a58ad7ee21da:88\",\"0a59b69a-7565-4f3c-a134-43c9c83ca593:861\",\"e3d9232f-e613-4c82-a067-a5220e4d83b4:61\",\"fd06b674-e505-45d7-a80e-27343048cf4a:61\",\"904081c7-a80e-4916-a7ab-88afa7dcaa0f:61\",\"c9e12bcc-8f07-4ee2-a1cd-d1aadc6688a8:61\",\"5d4e56df-ff31-47a6-ae37-fe6482ccf2f1:61\",\"355c9fa2-ae89-4fb9-acab-1b74e60da6cf:75\",\"ca57d311-2ba7-4c2f-adc3-19bb9f7bf657:61\"],\"consistencyIssues\":false}}";
                assertEquals(expected, json);
                verify(entryTrampoline, times(2)).submitForSaving(any());
            }
        }
    }
}
