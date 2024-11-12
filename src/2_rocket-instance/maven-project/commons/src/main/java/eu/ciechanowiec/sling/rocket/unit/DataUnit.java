package eu.ciechanowiec.sling.rocket.unit;

import java.util.function.UnaryOperator;

/**
 * Units of digital information.
 *
 * <table>
 *   <caption>Data Unit Sizes</caption>
 *   <tr>
 *     <th>Constant</th>
 *     <th>Data size</th>
 *     <th>Power&nbsp;of&nbsp;2</th>
 *     <th>Size in Bytes</th>
 *   </tr>
 *   <tr>
 *     <td>{@link DataUnit#BYTES}</td>
 *     <td>1B</td>
 *     <td>2^0</td>
 *     <td>1</td>
 *   </tr>
 *   <tr>
 *     <td>{@link DataUnit#KILOBYTES}</td>
 *     <td>1KB</td>
 *     <td>2^10</td>
 *     <td>1,024</td>
 *   </tr>
 *   <tr>
 *     <td>{@link DataUnit#MEGABYTES}</td>
 *     <td>1MB</td>
 *     <td>2^20</td>
 *     <td>1,048,576</td>
 *   </tr>
 *   <tr>
 *     <td>{@link DataUnit#GIGABYTES}</td>
 *     <td>1GB</td>
 *     <td>2^30</td>
 *     <td>1,073,741,824</td>
 *   </tr>
 *   <tr>
 *     <td>{@link DataUnit#TERABYTES}</td>
 *     <td>1TB</td>
 *     <td>2^40</td>
 *     <td>1,099,511,627,776</td>
 *   </tr>
 * </table>
 */
public enum DataUnit {

    /**
     * Bytes (B).
     */
    BYTES(UnaryOperator.identity()),

    /**
     * Kilobytes (KB).
     */
    KILOBYTES(kilobytes -> Math.multiplyExact(kilobytes, DataUnitMultiplications.BYTES_PER_KB)),

    /**
     * Megabytes (MB).
     */
    MEGABYTES(megabytes -> Math.multiplyExact(megabytes, DataUnitMultiplications.BYTES_PER_MB)),

    /**
     * Gigabytes (GB).
     */
    GIGABYTES(gigabytes -> Math.multiplyExact(gigabytes, DataUnitMultiplications.BYTES_PER_GB)),

    /**
     * Terabytes (TB).
     */
    TERABYTES(terabytes -> Math.multiplyExact(terabytes, DataUnitMultiplications.BYTES_PER_TB));

    private final UnaryOperator<Long> toBytes;

    DataUnit(UnaryOperator<Long> toBytes) {
        this.toBytes = toBytes;
    }

    long toBytes(long size) {
        return toBytes.apply(size);
    }
}
