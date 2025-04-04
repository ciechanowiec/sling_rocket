package eu.ciechanowiec.sling.rocket.unit;

import eu.ciechanowiec.conditional.Conditional;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.function.Supplier;

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
@SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject", "WeakerAccess"})
@Slf4j
public final class DataSize implements Comparable<DataSize> {

    private final MemoizingSupplier<Long> bytesSupplier;

    private DataSize(long bytes) {
        this.bytesSupplier = new MemoizingSupplier<>(() -> bytes);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param amount   amount of the size of digital information represented by this {@link DataSize}, measured in the
     *                 specified {@link DataUnit}
     * @param dataUnit unit of digital information that the specified amount is measured in
     */
    public DataSize(long amount, DataUnit dataUnit) {
        this(dataUnit.toBytes(amount));
    }

    /**
     * Constructs an instance of this class.
     *
     * @param file {@link File} whose size is represented by this {@link DataSize}
     */
    public DataSize(File file) {
        this(() -> file);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param fileSupplier {@link Supplier} with a {@link File} whose size is represented by this {@link DataSize}
     */
    public DataSize(Supplier<File> fileSupplier) {
        this.bytesSupplier = new MemoizingSupplier<>(() -> fileSupplier.get().length());
        Conditional.onFalseExecute(
            fileSupplier.get().exists(), () -> log.warn("This file doesn't exist: '{}'", fileSupplier)
        );
    }

    /**
     * Number of {@link DataUnit#BYTES} in this {@link DataSize}.
     *
     * @return the number of bytes
     */
    public long bytes() {
        return bytesSupplier.get();
    }

    /**
     * Number of {@link DataUnit#KILOBYTES} in this {@link DataSize}.
     *
     * @return number of {@link DataUnit#KILOBYTES} in this {@link DataSize}
     */
    public double kilobytes() {
        return (double) bytesSupplier.get() / DataUnitMultiplications.BYTES_PER_KB;
    }

    /**
     * Number of {@link DataUnit#MEGABYTES} in this {@link DataSize}.
     *
     * @return the number of bytes
     */
    public double megabytes() {
        return (double) bytesSupplier.get() / DataUnitMultiplications.BYTES_PER_MB;
    }

    /**
     * Number of {@link DataUnit#GIGABYTES} in this {@link DataSize}.
     *
     * @return the number of bytes
     */
    public double gigabytes() {
        return (double) bytesSupplier.get() / DataUnitMultiplications.BYTES_PER_GB;
    }

    /**
     * Number of {@link DataUnit#TERABYTES} in this {@link DataSize}.
     *
     * @return the number of bytes
     */
    public double terabytes() {
        return (double) bytesSupplier.get() / DataUnitMultiplications.BYTES_PER_TB;
    }

    /**
     * Checks if this {@link DataSize} is bigger than the compared {@link DataSize}.
     *
     * @param comparedDataSize {@link DataSize} to which this {@link DataSize} is compared to
     * @return {@code true} if this {@link DataSize} is bigger than the compared {@link DataSize}; {@code false}
     * otherwise
     */
    public boolean biggerThan(DataSize comparedDataSize) {
        return bytesSupplier.get() > comparedDataSize.bytesSupplier.get();
    }

    /**
     * Checks if this {@link DataSize} is smaller than the compared {@link DataSize}.
     *
     * @param comparedDataSize {@link DataSize} to which this {@link DataSize} is compared to
     * @return {@code true} if this {@link DataSize} is smaller than the compared {@link DataSize}; {@code false}
     * otherwise
     */
    public boolean smallerThan(DataSize comparedDataSize) {
        return bytesSupplier.get() < comparedDataSize.bytesSupplier.get();
    }

    @Override
    public int compareTo(DataSize comparedDataSize) {
        return Long.compare(bytesSupplier.get(), comparedDataSize.bytesSupplier.get());
    }

    /**
     * Adds the specified {@link DataSize} to this {@link DataSize}. The sum result is returned and this
     * {@link DataSize} remains unchanged.
     *
     * @param dataSizeToAdd {@link DataSize} to add to this {@link DataSize}
     * @return new {@link DataSize} that is the sum of this {@link DataSize} and the passed {@link DataSize}
     */
    public DataSize add(DataSize dataSizeToAdd) {
        long thisBytes = this.bytes();
        long bytesToAdd = dataSizeToAdd.bytes();
        return new DataSize(thisBytes + bytesToAdd);
    }

    @Override
    @SuppressWarnings({"SimplifiableIfStatement", "PMD.SimplifyBooleanReturns"})
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        return comparedObject instanceof DataSize comparedDataSize
            && bytesSupplier.get().equals(comparedDataSize.bytesSupplier.get());
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bytesSupplier.get());
    }

    @Override
    @SuppressWarnings("VariableDeclarationUsageDistance")
    public String toString() {
        long remainingBytes = bytesSupplier.get();

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
            "[%d TB, %d GB, %d MB, %d KB, %d B (total: %d bytes)]",
            hrTerabytes, hrGigabytes, hrMegabytes, hrKilobytes, hrBytes, bytesSupplier.get()
        );
    }
}
