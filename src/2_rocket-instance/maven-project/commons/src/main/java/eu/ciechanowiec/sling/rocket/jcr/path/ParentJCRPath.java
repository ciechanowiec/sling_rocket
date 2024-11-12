package eu.ciechanowiec.sling.rocket.jcr.path;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.jcr.Item;
import javax.jcr.Repository;

/**
 * Represents a parent path of the persisted or hypothetically persisted {@link Item} in the {@link Repository},
 * upon which some action is to be performed.
 */
@ToString
@Slf4j
public class ParentJCRPath implements JCRPath {

    private final JCRPath source;

    /**
     * Constructs an instance of this class using a source JCR path.
     * @param source JCR path to be represented by the constructed object
     */
    public ParentJCRPath(JCRPath source) {
        this.source = source;
        log.trace("Initialized {}", this);
    }

    @Override
    public String get() {
        return source.get();
    }

    @Override
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        if (comparedObject instanceof JCRPath comparedJCRPath) {
            String thisRawJCRPath = source.get();
            String thatRawJCRPath = comparedJCRPath.get();
            return thisRawJCRPath.equals(thatRawJCRPath);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return source.get().hashCode() * 31;
    }
}
