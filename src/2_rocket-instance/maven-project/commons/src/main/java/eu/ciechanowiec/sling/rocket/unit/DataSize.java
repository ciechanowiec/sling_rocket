package eu.ciechanowiec.sling.rocket.unit;

import eu.ciechanowiec.conditional.Conditional;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Size of digital information.
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
@ToString
@SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject", "WeakerAccess"})
@Slf4j
public final class DataSize implements Comparable<DataSize> {

    private final CompletableFuture<Long> bytesFuture;

    private DataSize(long bytes) {
        this.bytesFuture = CompletableFuture.supplyAsync(() -> bytes);
    }

    /**
     * Constructs an instance of this class.
     * @param amount amount of the size of digital information represented by this {@link DataSize},
     *               measured in the specified {@link DataUnit}
     * @param dataUnit unit of digital information that the specified amount is measured in
     */
    public DataSize(long amount, DataUnit dataUnit) {
        this(dataUnit.toBytes(amount));
    }

    /**
     * Constructs an instance of this class.
     * @param file {@link File} whose size is represented by this {@link DataSize}
     */
    public DataSize(File file) {
        this.bytesFuture = CompletableFuture.supplyAsync(file::length);
        Conditional.onFalseExecute(file.exists(), () -> log.warn("This file doesn't exist: '{}'", file));
    }

    /**
     * Number of {@link DataUnit#BYTES} in this {@link DataSize}.
     * @return the number of bytes
     */
    public long bytes() {
        return bytesFuture.join();
    }

    /**
     * Number of {@link DataUnit#KILOBYTES} in this {@link DataSize}.
     * @return number of {@link DataUnit#KILOBYTES} in this {@link DataSize}
     */
    public double kilobytes() {
        return (double) bytesFuture.join() / DataUnitMultiplications.BYTES_PER_KB;
    }

    /**
     * Number of {@link DataUnit#MEGABYTES} in this {@link DataSize}.
     * @return the number of bytes
     */
    public double megabytes() {
        return (double) bytesFuture.join() / DataUnitMultiplications.BYTES_PER_MB;
    }

    /**
     * Number of {@link DataUnit#GIGABYTES} in this {@link DataSize}.
     * @return the number of bytes
     */
    public double gigabytes() {
        return (double) bytesFuture.join() / DataUnitMultiplications.BYTES_PER_GB;
    }

    /**
     * Number of {@link DataUnit#TERABYTES} in this {@link DataSize}.
     * @return the number of bytes
     */
    public double terabytes() {
        return (double) bytesFuture.join() / DataUnitMultiplications.BYTES_PER_TB;
    }

    /**
     * Checks if this {@link DataSize} is bigger than the compared {@link DataSize}.
     * @param comparedDataSize {@link DataSize} to which this {@link DataSize} is compared to
     * @return {@code true} if this {@link DataSize} is bigger than the compared {@link DataSize};
     *         {@code false} otherwise
     */
    public boolean biggerThan(DataSize comparedDataSize) {
        return bytesFuture.join() > comparedDataSize.bytesFuture.join();
    }

    /**
     * Checks if this {@link DataSize} is smaller than the compared {@link DataSize}.
     * @param comparedDataSize {@link DataSize} to which this {@link DataSize} is compared to
     * @return {@code true} if this {@link DataSize} is smaller than the compared {@link DataSize};
     *         {@code false} otherwise
     */
    public boolean smallerThan(DataSize comparedDataSize) {
        return bytesFuture.join() < comparedDataSize.bytesFuture.join();
    }

    @Override
    public int compareTo(DataSize comparedDataSize) {
        return Long.compare(bytesFuture.join(), comparedDataSize.bytesFuture.join());
    }

    @Override
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        if (Objects.isNull(comparedObject) || getClass() != comparedObject.getClass()) {
            return false;
        }
        DataSize comparedDataSize = (DataSize) comparedObject;
        return Objects.equals(bytesFuture.join(), comparedDataSize.bytesFuture.join());
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bytesFuture.join());
    }

    /**
     * Returns a human-readable representation of this {@link DataSize}.
     * @return human-readable representation of this {@link DataSize}
     */
    @SuppressWarnings("VariableDeclarationUsageDistance")
    public String toHumanReadableRepresentation() {
        long remainingBytes = bytesFuture.join();

        long hrTerabytes = remainingBytes / DataUnitMultiplications.BYTES_PER_TB;
        remainingBytes %= DataUnitMultiplications.BYTES_PER_TB;

        long hrGigabytes = remainingBytes / DataUnitMultiplications.BYTES_PER_GB;
        remainingBytes %= DataUnitMultiplications.BYTES_PER_GB;

        long hrMegabytes = remainingBytes / DataUnitMultiplications.BYTES_PER_MB;
        remainingBytes %= DataUnitMultiplications.BYTES_PER_MB;

        long hrKilobytes = remainingBytes / DataUnitMultiplications.BYTES_PER_KB;
        remainingBytes %= DataUnitMultiplications.BYTES_PER_KB;

        long hrBytes = remainingBytes;

        return String.format(
                "[%d TB, %d GB, %d MB, %d KB, %d B]", hrTerabytes, hrGigabytes, hrMegabytes, hrKilobytes, hrBytes
        );
    }
}
