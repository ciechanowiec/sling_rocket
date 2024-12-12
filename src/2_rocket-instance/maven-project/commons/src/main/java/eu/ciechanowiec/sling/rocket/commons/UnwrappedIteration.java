package eu.ciechanowiec.sling.rocket.commons;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Data structure operations on the wrapped {@link Iterator} and {@link Iterable}.
 *
 * @param <T> the type of elements in the wrapped data structure
 */
@SuppressWarnings("WeakerAccess")
public class UnwrappedIteration<T> {

    private final MemoizingSupplier<Collection<T>> unwrappedCollection;

    /**
     * Constructs an instance of this class.
     * @param iterator {@link Iterator} to be unwrapped
     */
    public UnwrappedIteration(Iterator<T> iterator) {
        unwrappedCollection = new MemoizingSupplier<>(() -> asCollection(iterator));
    }

    /**
     * Constructs an instance of this class.
     * @param iterable {@link Iterable} to be unwrapped
     */
    public UnwrappedIteration(Iterable<T> iterable) {
        unwrappedCollection = new MemoizingSupplier<>(() -> asCollection(iterable));
    }

    private Collection<T> asCollection(Iterator<T> iterator) {
        return unwrap(iterator).toList();
    }

    private Collection<T> asCollection(Iterable<T> iterable) {
        return unwrap(iterable).toList();
    }

    private Stream<T> unwrap(Iterator<T> iterator) {
        Iterable<T> iterable = () -> iterator;
        return unwrap(iterable);
    }

    private Stream<T> unwrap(Iterable<T> iterable) {
        Spliterator<T> spliterator = iterable.spliterator();
        return StreamSupport.stream(spliterator, false);
    }

    /**
     * Returns a {@link Stream} of elements from the wrapped data structure.
     *
     * @return {@link Stream} of elements from the wrapped data structure
     */
    public Stream<T> stream() {
        Collection<T> collection = unwrappedCollection.get();
        return collection.stream();
    }

    /**
     * Returns an unmodifiable {@link List} of elements from the wrapped data structure.
     * A new object is returned each time this method is called.
     *
     * @return unmodifiable {@link List} of elements from the wrapped data structure
     */
    public List<T> list() {
        return stream().toList();
    }
}
