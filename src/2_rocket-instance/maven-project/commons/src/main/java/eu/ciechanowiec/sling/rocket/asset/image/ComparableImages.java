package eu.ciechanowiec.sling.rocket.asset.image;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collection of {@link ComparableImage}-s.
 *
 * @param source underlying instances of {@link ComparableImage}-s
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
@SuppressWarnings("WeakerAccess")
public record ComparableImages(Collection<ComparableImage> source) {

    /**
     * Returns the underlying {@link ComparableImage}-s as {@link File}-s.
     *
     * @return underlying {@link ComparableImage}-s as {@link File}-s
     */
    public Collection<File> asFiles() {
        return source.stream()
            .map(ComparableImage::fileWithImage)
            .toList();
    }

    /**
     * <p>
     * Returns a new instance of {@link ComparableImages} with similar images excluded.
     * </p>
     * Useful specifically in cases where this {@link ComparableImages} contains instances of {@link ComparableImage}-s
     * which represent the same image but with different resolutions or other properties, although it isn't completely
     * guaranteed that in such cases all similar images will be excluded.
     *
     * @return new instance of {@link ComparableImages} with similar images excluded
     */
    public ComparableImages excludeSimilarImages() {
        List<ComparableImage> withoutSimilarImages = source.stream()
            .collect(Collectors.toUnmodifiableMap(
                    Function.identity(),
                    comparableImage -> extractSimilarImages(comparableImage, source)
                )
            )
            .entrySet()
            .stream()
            .map(similarImages -> {
                ComparableImage keyImage = similarImages.getKey();
                Collection<ComparableImage> valueImages = similarImages.getValue();
                return Stream.concat(Stream.of(keyImage), valueImages.stream())
                    .distinct()
                    .toList();
            })
            .map(similarImages -> similarImages.stream().reduce(ComparableImage::biggerOrSame))
            .flatMap(Optional::stream)
            .distinct()
            .toList();
        return new ComparableImages(withoutSimilarImages);
    }

    private Collection<ComparableImage> extractSimilarImages(
        ComparableImage referenceImage, Collection<ComparableImage> imagesToExtractFrom
    ) {
        return imagesToExtractFrom.stream()
            .filter(referenceImage::isSimilar)
            .toList();
    }
}
