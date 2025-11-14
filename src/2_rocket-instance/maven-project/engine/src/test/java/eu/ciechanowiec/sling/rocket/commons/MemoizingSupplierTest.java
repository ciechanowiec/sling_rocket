package eu.ciechanowiec.sling.rocket.commons;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class MemoizingSupplierTest {

    @Test
    void testWasComputed() {
        // Counter to verify the delegate is called only once
        AtomicInteger counter = new AtomicInteger(0);

        // Create a delegate Supplier
        Supplier<String> delegate = () -> {
            counter.incrementAndGet();
            return "Computed Value";
        };

        // Create the MemoizingSupplier
        MemoizingSupplier<String> memoizingSupplier = new MemoizingSupplier<>(delegate);

        // Before calling get(), wasComputed() should return false
        assertFalse(memoizingSupplier.wasComputed());

        // Call get() for the first time
        String value1 = memoizingSupplier.get();

        // Check the returned value and ensure wasComputed() is true
        assertEquals("Computed Value", value1);
        assertTrue(memoizingSupplier.wasComputed());

        // Call get() again to confirm the cached value is returned
        String value2 = memoizingSupplier.get();

        // Ensure the value is the same and the delegate was only called once
        assertEquals("Computed Value", value2);
        assertEquals(1, counter.get());

        // wasComputed() should still return true
        assertTrue(memoizingSupplier.wasComputed());
    }
}
