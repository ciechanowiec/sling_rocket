package eu.ciechanowiec.sling.rocket.commons;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("MagicNumber")
class UnwrappedIterationTest {

    private List<Integer> sampleList;

    @BeforeEach
    void setup() {
        sampleList = Arrays.asList(1, 2, 3, 4, 5);
    }

    @Test
    void testUnwrappedIterationWithIterator() {
        Iterator<Integer> iterator = sampleList.iterator();
        UnwrappedIteration<Integer> unwrappedIteration = new UnwrappedIteration<>(iterator);
        List<Integer> result = unwrappedIteration.stream().toList();
        assertEquals(sampleList, result);
    }

    @Test
    void testUnwrappedIterationWithIterable() {
        Iterable<Integer> iterable = sampleList;
        UnwrappedIteration<Integer> unwrappedIteration = new UnwrappedIteration<>(iterable);
        List<Integer> result = unwrappedIteration.stream().toList();
        assertEquals(sampleList, result);
    }

    @Test
    void testUnwrappedIterationWithEmptyIterator() {
        Iterator<Integer> iterator = Collections.emptyIterator();
        UnwrappedIteration<Integer> unwrappedIteration = new UnwrappedIteration<>(iterator);
        List<Integer> result = unwrappedIteration.stream().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    void testUnwrappedIterationWithEmptyIterable() {
        Iterable<Integer> iterable = List.of();
        UnwrappedIteration<Integer> unwrappedIteration = new UnwrappedIteration<>(iterable);
        List<Integer> result = unwrappedIteration.stream().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    void testUnwrappedIterationWithSingleElementIterator() {
        List<Integer> singleElementList = List.of(42);
        Iterator<Integer> iterator = singleElementList.iterator();
        UnwrappedIteration<Integer> unwrappedIteration = new UnwrappedIteration<>(iterator);
        List<Integer> result = unwrappedIteration.stream().toList();
        assertEquals(singleElementList, result);
    }

    @Test
    void testUnwrappedIterationWithSingleElementIterable() {
        List<Integer> singleElementList = List.of(42);
        UnwrappedIteration<Integer> unwrappedIteration = new UnwrappedIteration<>(singleElementList);
        List<Integer> result = unwrappedIteration.stream().toList();
        assertEquals(singleElementList, result);
    }

    @Test
    void testStreamReturnsNewStreamEachTime() {
        UnwrappedIteration<Integer> unwrappedIteration = new UnwrappedIteration<>(sampleList);
        Stream<Integer> stream1 = unwrappedIteration.stream();
        Stream<Integer> stream2 = unwrappedIteration.stream();
        assertNotSame(stream1, stream2);
    }

    @Test
    void testStreamDoesNotModifyOriginalCollection() {
        UnwrappedIteration<Integer> unwrappedIteration = new UnwrappedIteration<>(sampleList);
        List<Integer> result = unwrappedIteration.stream().collect(Collectors.toCollection(ArrayList::new));
        assertEquals(sampleList, result);
        // Modify the result list and verify that the original sampleList is not modified
        result.add(6);
        assertNotEquals(sampleList.size(), result.size());
        assertEquals(List.of(1, 2, 3, 4, 5), sampleList);
    }
}
