package eu.ciechanowiec.sling.rocket.jcr.path;

import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a path to the persisted or hypothetically persisted {@link Item} in the {@link Repository}, upon which
 * some action is to be performed.
 */
@ToString
@Slf4j
@SuppressWarnings("squid:S1192")
public class TargetJCRPath implements JCRPath {

    private final String rawPath;

    /**
     * Constructs an instance of this class using a raw JCR path.
     *
     * @param rawPath valid JCR path to be represented by the constructed object
     */
    public TargetJCRPath(String rawPath) {
        this.rawPath = rawPath;
        log.trace("Initialized {}", this);
    }

    /**
     * Constructs an instance of this class that will represent a JCR path to the specified {@link Resource}. The
     * specified {@link Resource} must be adaptable to an existing {@link Node} when the instance is constructed.
     *
     * @param resource {@link Resource} adaptable to a {@link Node} whose JCR path will be represented by the
     *                 constructed object
     */
    @SneakyThrows
    public TargetJCRPath(Resource resource) {
        Node node = Optional.ofNullable(resource.adaptTo(Node.class)).orElseThrow(
            () -> {
                String message = "%s does not adapt to Node".formatted(resource);
                return new IllegalArgumentException(message);
            }
        );
        this.rawPath = node.getPath();
        log.trace("Initialized {}", this);
    }

    /**
     * Constructs an instance of this class using a parent JCR path and a child node name.
     *
     * @param parentJCRPath JCR path that points to the direct parent of an {@link Item} at the JCR path to be
     *                      represented by the constructed object
     * @param childJCRName  name of the lowest {@link Item} at the JCR path to be represented by the constructed object
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public TargetJCRPath(ParentJCRPath parentJCRPath, String childJCRName) {
        String parentJCRPathAsString = parentJCRPath.get();
        this.rawPath = String.format("%s/%s", parentJCRPathAsString, childJCRName);
        log.trace("Initialized {}", this);
    }

    /**
     * Constructs an instance of this class using a parent JCR path and a child node name.
     *
     * @param parentJCRPath JCR path that points to the direct parent of an {@link Item} at the JCR path to be
     *                      represented by the constructed object
     * @param childJCRName  name of the lowest {@link Item} at the JCR path to be represented by the constructed object
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public TargetJCRPath(ParentJCRPath parentJCRPath, UUID childJCRName) {
        String parentJCRPathAsString = parentJCRPath.get();
        String childJCRNameAsString = childJCRName.toString();
        this.rawPath = String.format("%s/%s", parentJCRPathAsString, childJCRNameAsString);
        log.trace("Initialized {}", this);
    }

    @Override
    public String get() {
        boolean isValidPath = PathUtils.isValid(rawPath);
        if (!isValidPath) {
            String message = String.format("Invalid JCR path: '%s'", rawPath);
            throw new InvalidJCRPathException(message);
        }
        return rawPath;
    }

    @Override
    public boolean equals(Object comparedObject) {
        if (this == comparedObject) {
            return true;
        }
        if (comparedObject instanceof JCRPath comparedJCRPath) {
            String thatRawJCRPath = comparedJCRPath.get();
            return rawPath.equals(thatRawJCRPath);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return rawPath.hashCode() * 31;
    }
}
