package eu.ciechanowiec.sling.rocket.unit;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("MagicNumber")
class DataSizeTest {

    @Test
    void testConstructorWithAmountAndUnit() {
        DataSize dataSize = new DataSize(2, DataUnit.KILOBYTES);
        assertEquals(2048L, dataSize.bytes());
    }

    @Test
    void testBytesConversion() {
        DataSize dataSizeMB = new DataSize(1, DataUnit.MEGABYTES);
        assertEquals(1_048_576L, dataSizeMB.bytes(), "1 MB should be 1,048,576 bytes");
        assertEquals(1024.0, dataSizeMB.kilobytes(), "1 MB should be 1024 KB");
        assertEquals(1.0, dataSizeMB.megabytes(), "1 MB should be 1 MB");
        assertEquals(1.0 / 1024.0, dataSizeMB.gigabytes(), 1e-6, "1 MB should be approximately 0.000976 GB");
        assertEquals(
            1.0 / (1024.0 * 1024.0), dataSizeMB.terabytes(), 1e-9, "1 MB should be approximately 9.53674316e-7 TB"
        );

        DataSize dataSizeTB = new DataSize(2, DataUnit.TERABYTES);
        assertEquals(
            2L * DataUnitMultiplications.BYTES_PER_TB, dataSizeTB.bytes(), "2 TB should be correct number of bytes"
        );
        assertEquals(2L * 1024L * 1024L * 1024L, dataSizeTB.kilobytes(), "2 TB should be 2,147,483,648 KB");
        assertEquals(2L * 1024L * 1024L, dataSizeTB.megabytes(), "2 TB should be 2,097,152 MB");
        assertEquals(2L * 1024L, dataSizeTB.gigabytes(), "2 TB should be 2,048 GB");
        assertEquals(2.0, dataSizeTB.terabytes(), "2 TB should be 2 TB");
    }

    @Test
    void testBiggerThan() {
        DataSize dataSize1 = new DataSize(1, DataUnit.GIGABYTES);
        DataSize dataSize2 = new DataSize(500, DataUnit.MEGABYTES);
        assertTrue(dataSize1.biggerThan(dataSize2), "1 GB should be bigger than 500 MB");
        assertFalse(dataSize2.biggerThan(dataSize1), "500 MB should not be bigger than 1 GB");
    }

    @Test
    void testSmallerThan() {
        DataSize dataSize1 = new DataSize(1, DataUnit.GIGABYTES);
        DataSize dataSize2 = new DataSize(500, DataUnit.MEGABYTES);
        assertTrue(dataSize2.smallerThan(dataSize1), "500 MB should be smaller than 1 GB");
        assertFalse(dataSize1.smallerThan(dataSize2), "1 GB should not be smaller than 500 MB");
    }

    @Test
    void testCompareTo() {
        DataSize dataSize1 = new DataSize(1, DataUnit.GIGABYTES);
        DataSize dataSize2 = new DataSize(1024, DataUnit.MEGABYTES);
        DataSize dataSize3 = new DataSize(2, DataUnit.GIGABYTES);

        assertEquals(0, dataSize1.compareTo(dataSize2), "1 GB should be equal to 1024 MB");
        assertTrue(dataSize1.compareTo(dataSize3) < 0, "1 GB should be less than 2 GB");
        assertTrue(dataSize3.compareTo(dataSize1) > 0, "2 GB should be greater than 1 GB");
    }

    @Test
    void testEqualsAndHashCode() {
        DataSize dataSize1 = new DataSize(1, DataUnit.GIGABYTES);
        DataSize dataSize2 = new DataSize(1024 * 1024 * 1024, DataUnit.BYTES);

        assertEquals(dataSize1, dataSize2, "DataSizes should be equal");
        assertEquals(dataSize1.hashCode(), dataSize2.hashCode(), "Hash codes should be equal");
    }

    @SneakyThrows
    @Test
    void testFileConstructor() {
        // Create a temporary file with known size
        File tempFile = File.createTempFile("testFile", ".tmp");
        tempFile.deleteOnExit();

        byte[] data = new byte[1024]; // 1 KB
        try (OutputStream fos = Files.newOutputStream(tempFile.toPath())) {
            IOUtils.write(data, fos);
        }

        DataSize dataSize = new DataSize(tempFile);
        assertEquals(1024L, dataSize.bytes(), "File size should be 1024 bytes");
    }

    @Test
    void testToString() {
        DataSize dataSize = new DataSize(345_448_245, DataUnit.BYTES);
        assertEquals("[0 TB, 0 GB, 329 MB, 455 KB, 821 B (total: 345448245 bytes)]", dataSize.toString());
    }

    @Test
    void testAddition() {
        List<DataSize> dataSizesOne = List.of(
            new DataSize(2, DataUnit.MEGABYTES),
            new DataSize(0, DataUnit.MEGABYTES),
            new DataSize(4, DataUnit.MEGABYTES),
            new DataSize(5, DataUnit.MEGABYTES)
        );
        DataSize nonZeroResult = dataSizesOne.stream().reduce(DataSize::add).orElse(new DataSize(0, DataUnit.BYTES));
        List<DataSize> dataSizesTwo = List.of();
        @SuppressWarnings("RedundantOperationOnEmptyContainer")
        DataSize zeroResult = dataSizesTwo.stream().reduce(DataSize::add).orElse(new DataSize(0, DataUnit.BYTES));
        assertAll(
            () -> assertEquals(new DataSize(11, DataUnit.MEGABYTES), nonZeroResult),
            () -> assertEquals(new DataSize(0, DataUnit.BYTES), zeroResult)
        );
    }

    @Test
    @SuppressWarnings("EqualsWithItself")
    void testSameObjectEquality() {
        DataSize dataSize = new DataSize(1, DataUnit.GIGABYTES);
        assertEquals(dataSize, dataSize);
    }
}
