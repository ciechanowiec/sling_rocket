package eu.ciechanowiec.sling.rocket.asset.image;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;
import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Represents an image that can be compared with other images.
 */
@Slf4j
@ToString
@EqualsAndHashCode
@SuppressWarnings("WeakerAccess")
public class ComparableImage {

    @SuppressWarnings({"FieldNamingConvention", "PMD.LongVariable"})
    private static final double DEFAULT_MAX_NORMALIZED_HAMMING_DISTANCE = 0.1; // Optimal after tests

    @Getter
    private final File fileWithImage;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final MemoizingSupplier<Optional<Hash>> memorizedHash;

    /**
     * Constructs an instance of this class.
     *
     * @param fileWithImage {@link File} with the underlying image; the client code must ensure that the {@link File} is
     *                      a valid image file since for other files there is no guarantee that they will be processed
     *                      correctly
     */
    public ComparableImage(File fileWithImage) {
        this.fileWithImage = fileWithImage;
        memorizedHash = new MemoizingSupplier<>(() -> computeHash(fileWithImage));
    }

    private Optional<Hash> computeHash(File file) {
        int bitResolution = BitResolution.MAX_256.value();
        HashingAlgorithm hashingAlgorithm = new PerceptiveHash(bitResolution);
        log.trace("Trying to hash {}", file);
        try {
            Hash hash = hashingAlgorithm.hash(file);
            log.trace("Hashed {} to {}", file, hash);
            return Optional.of(hash);
        } catch (IOException | IllegalArgumentException exception) {
            String errorMessage = String.format("Unable to hash %s", file);
            log.trace(errorMessage, exception);
            return Optional.empty();
        }
    }

    private Optional<Hash> lastComputedHash() {
        return memorizedHash.get();
    }

    ComparableImage biggerOrSame(ComparableImage comparedImage) {
        long thisSize = fileWithImage.length();
        @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
        long thatSize = comparedImage.fileWithImage.length();
        if (thatSize > thisSize) {
            return comparedImage;
        } else {
            return this;
        }
    }

    boolean isSimilar(ComparableImage comparedImage) {
        log.trace("Comparing if {} and {} are similar", this, comparedImage);
        Optional<Hash> firstHashNullable = this.lastComputedHash();
        Optional<Hash> secondHashNullable = comparedImage.lastComputedHash();
        boolean bothHashesExist = firstHashNullable.isPresent() && secondHashNullable.isPresent();
        if (!bothHashesExist) {
            log.trace("{} and {} are not similar", this, comparedImage);
            return false;
        }
        Hash firstHash = firstHashNullable.get();
        Hash secondHash = secondHashNullable.get();
        double normalizedHammingDistance = firstHash.normalizedHammingDistance(secondHash);
        boolean areSimilar = normalizedHammingDistance <= DEFAULT_MAX_NORMALIZED_HAMMING_DISTANCE;
        log.trace(
            "Distance between {} and {} is {}. Max allowed distance for similarity is {}. "
                + "Are images similar? Answer: {}", this, comparedImage, normalizedHammingDistance,
            DEFAULT_MAX_NORMALIZED_HAMMING_DISTANCE, areSimilar
        );
        return areSimilar;
    }
}
