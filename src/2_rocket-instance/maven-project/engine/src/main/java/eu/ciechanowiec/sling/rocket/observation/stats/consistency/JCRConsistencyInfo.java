package eu.ciechanowiec.sling.rocket.observation.stats.consistency;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

class JCRConsistencyInfo implements JSON {

    private final Path segmentStoreAbsPath;
    private final MemoizingSupplier<Optional<String>> lastValidSegment;
    private final MemoizingSupplier<Optional<List<String>>> journalLogRecentSegments;

    JCRConsistencyInfo(
        Path segmentStoreAbsPath,
        Supplier<Optional<String>> lastValidSegment,
        Supplier<Optional<List<String>>> journalLogRecentSegments
    ) {
        this.segmentStoreAbsPath = segmentStoreAbsPath;
        this.lastValidSegment = new MemoizingSupplier<>(lastValidSegment);
        this.journalLogRecentSegments = new MemoizingSupplier<>(journalLogRecentSegments);
    }

    @JsonProperty
    @JsonSerialize(using = ToStringSerializer.class)
    private Path segmentStoreAbsPath() {
        return segmentStoreAbsPath;
    }

    @JsonProperty
    private String lastValidSegment() {
        return lastValidSegment.get().orElse("UNKNOWN");
    }

    @JsonProperty
    private List<String> journalLogRecentSegments() {
        return journalLogRecentSegments.get().orElse(List.of());
    }

    @JsonProperty
    private boolean consistencyIssues() {
        return lastValidSegment.get().map(
            validSegment -> journalLogRecentSegments.get().map(
                recentSegments -> !recentSegments.contains(validSegment)
            ).orElse(true)
        ).orElse(true);
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        return objectMapper.writer(printer).writeValueAsString(this);
    }
}
