package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import lombok.SneakyThrows;

class DiskStats implements JSON {

    private final DataSize totalSpace;
    private final DataSize occupiedSpace;
    private final DataSize usableSpace;

    DiskStats(DataSize totalSpace, DataSize occupiedSpace, DataSize usableSpace) {
        this.totalSpace = totalSpace;
        this.occupiedSpace = occupiedSpace;
        this.usableSpace = usableSpace;
    }

    @JsonProperty("totalSpace")
    String totalSpace() {
        return totalSpace.toString();
    }

    @JsonProperty("occupiedSpace")
    String occupiedSpace() {
        String occupiedPercentage = String.format("%.2f", (double) occupiedSpace.bytes() / totalSpace.bytes() * 100);
        return String.format("%s (%s%% of total space)", occupiedSpace, occupiedPercentage);
    }

    @JsonProperty("usableSpace")
    String usableSpace() {
        String freePercentage = String.format("%.2f", (double) usableSpace.bytes() / totalSpace.bytes() * 100);
        return String.format("%s (%s%% of total space)", usableSpace, freePercentage);
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}
