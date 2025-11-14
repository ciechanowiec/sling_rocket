package eu.ciechanowiec.sling.rocket.unit;

import lombok.experimental.UtilityClass;

@UtilityClass
class DataUnitMultiplications {

    /**
     * Bytes per Kilobyte.
     */
    static final long BYTES_PER_KB = 1024;

    /**
     * Bytes per Megabyte.
     */
    static final long BYTES_PER_MB = BYTES_PER_KB * 1024;

    /**
     * Bytes per Gigabyte.
     */
    static final long BYTES_PER_GB = BYTES_PER_MB * 1024;

    /**
     * Bytes per Terabyte.
     */
    static final long BYTES_PER_TB = BYTES_PER_GB * 1024;
}
