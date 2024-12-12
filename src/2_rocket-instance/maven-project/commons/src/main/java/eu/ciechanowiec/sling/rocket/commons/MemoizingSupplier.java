package eu.ciechanowiec.sling.rocket.commons;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A thread-safe memoizing {@link Supplier}.
 * <p>
 * The {@link MemoizingSupplier} caches the value returned upon the first invocation of {@link Supplier#get()}
 * on the delegate {@link Supplier} and subsequently returns the cached value. This is particularly useful when the
 * delegate {@link Supplier}'s computation is expensive or non-idempotent.
 * @param <T> the type of the value returned by the {@link MemoizingSupplier}
 */
public class MemoizingSupplier<T> implements Supplier<T> {

    /**
     * The underlying {@link Supplier} that provides the value to be memoized.
     */
    private final Supplier<T> delegate;

    /**
     * An atomic flag indicating whether the value has already been initialized.
     */
    private final AtomicBoolean wasInitialized;

    /**
     * A reference to hold the computed value once initialized.
     */
    private final AtomicReference<T> computedValue;

    /**
     * A lock to ensure thread-safe initialization of the value.
     */
    private final Lock lock;

    /**
     * Constructs an instance of this class.
     *
     * @param delegate {@link Supplier} whose value is to be memoized
     */
    public MemoizingSupplier(Supplier<T> delegate) {
        this.delegate = delegate;
        this.wasInitialized = new AtomicBoolean(false);
        this.computedValue = new AtomicReference<>();
        this.lock = new ReentrantLock();
    }

    /**
     * Returns the memoized value if it has already been computed, or computes and caches the value
     * from the underlying {@link Supplier} if this is the first invocation.
     * <p>
     * This method is thread-safe and ensures that the delegate {@link Supplier} is invoked at most once,
     * even in the presence of multiple threads.
     * <p>
     * Example usage:
     * <pre>
     *     Supplier&lt;String&gt; expensiveComputation = () -&gt; {
     *         // Some expensive computation
     *         return "Computed Value";
     *     };
     *     MemoizingSupplier&lt;String&gt; memoizingSupplier = new MemoizingSupplier&lt;&gt;(expensiveComputation);
     *
     *     // The delegate is invoked only once, subsequent calls return the cached result
     *     String value1 = memoizingSupplier.get(); // Computes and caches the value
     *     String value2 = memoizingSupplier.get(); // Returns the cached value
     * </pre>
     * @return the memoized value, either computed from the delegate or retrieved from the cache
     */
    @Override
    public T get() {
        if (wasInitialized.get()) {
            return computedValue.get();
        }
        lock.lock();
        try {
            if (!wasInitialized.getAndSet(true)) {
                T valueFromDelegate = delegate.get();
                this.computedValue.set(valueFromDelegate);
            }
        } finally {
            lock.unlock();
        }
        return computedValue.get();
    }
}
