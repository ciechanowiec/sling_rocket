package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import lombok.SneakyThrows;

class DiskCalculation implements JSON {

    private final DataSize totalSpace;
    private final DataSize occupiedSpace;
    private final DataSize usableSpace;

    DiskCalculation(DataSize totalSpace, DataSize occupiedSpace, DataSize usableSpace) {
        this.totalSpace = totalSpace;
        this.occupiedSpace = occupiedSpace;
        this.usableSpace = usableSpace;
    }

    @JsonProperty
    long totalSpaceBytes() {
        return totalSpace.bytes();
    }

    @JsonProperty
    String totalSpaceReadable() {
        return totalSpace.toString();
    }

    @JsonProperty
    long occupiedSpaceBytes() {
        return occupiedSpace.bytes();
    }

    @JsonProperty
    String occupiedSpaceReadable() {
        String occupiedPercentage = "%.2f".formatted((double) occupiedSpace.bytes() / totalSpace.bytes() * 100);
        return "%s (%s%% of total space)".formatted(occupiedSpace, occupiedPercentage);
    }

    @JsonProperty
    long usableSpaceBytes() {
        return usableSpace.bytes();
    }

    @JsonProperty
    String usableSpaceReadable() {
        String freePercentage = "%.2f".formatted((double) usableSpace.bytes() / totalSpace.bytes() * 100);
        return "%s (%s%% of total space)".formatted(usableSpace, freePercentage);
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}
