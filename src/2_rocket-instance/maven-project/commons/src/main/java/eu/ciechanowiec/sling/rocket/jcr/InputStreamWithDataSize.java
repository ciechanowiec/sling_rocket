package eu.ciechanowiec.sling.rocket.jcr;

import eu.ciechanowiec.sling.rocket.commons.MemoizingSupplier;
import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.unit.DataSize;
import eu.ciechanowiec.sling.rocket.unit.DataUnit;
import eu.ciechanowiec.sling.rocket.unit.WithDataSize;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * An {@link InputStream} that has a known {@link DataSize}.
 */
@Slf4j
@SuppressWarnings({"StandardVariableNames", "PMD.ShortVariable"})
public class InputStreamWithDataSize extends InputStream implements WithDataSize {

    @SuppressWarnings("PMD.LongVariable")
    private final JCRPath jcrPathToNodeWithBinaryProperty;
    private final MemoizingSupplier<ResourceResolver> resourceResolverSupplier;
    private final MemoizingSupplier<Optional<Binary>> binarySupplier;
    private final MemoizingSupplier<Optional<InputStream>> inputStreamSupplier;

    InputStreamWithDataSize(
        @SuppressWarnings("PMD.LongVariable")
        JCRPath jcrPathToNodeWithBinaryProperty,
        String binaryPropertyName, ResourceAccess resourceAccess
    ) {
        this.jcrPathToNodeWithBinaryProperty = jcrPathToNodeWithBinaryProperty;
        this.resourceResolverSupplier = new MemoizingSupplier<>(resourceAccess::acquireAccess);
        this.binarySupplier = new MemoizingSupplier<>(
            () -> binary(jcrPathToNodeWithBinaryProperty, binaryPropertyName, resourceResolverSupplier)
        );
        this.inputStreamSupplier = new MemoizingSupplier<>(
            () -> binarySupplier.get().map(SneakyFunction.sneaky(Binary::getStream))
        );
    }

    @SuppressWarnings("PMD.CloseResource")
    private Optional<Binary> binary(
        @SuppressWarnings("PMD.LongVariable")
        JCRPath jcrPathToNodeWithBinaryProperty, String binaryPropertyName,
        MemoizingSupplier<ResourceResolver> resourceResolverSupplier
    ) {
        ResourceResolver resourceResolver = resourceResolverSupplier.get();
        String jcrPathRaw = jcrPathToNodeWithBinaryProperty.get();
        return Optional.ofNullable(resourceResolver.getResource(jcrPathRaw))
            .flatMap(resource -> Optional.ofNullable(resource.adaptTo(Node.class)))
            .flatMap(node -> new ConditionalProperty(binaryPropertyName).retrieveFrom(node))
            .map(SneakyFunction.sneaky(Property::getValue))
            .flatMap(this::asBinary);
    }

    @Override
    public void close() {
        inputStreamSupplier.get().ifPresent(SneakyConsumer.sneaky(InputStream::close));
        binarySupplier.get().ifPresent(SneakyConsumer.sneaky(Binary::dispose));
        resourceResolverSupplier.get().close();
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public void mark(int readlimit) {
        inputStreamSupplier.get()
            .ifPresent(inputStream -> inputStream.mark(readlimit));
    }

    @Override
    public void reset() {
        inputStreamSupplier.get()
            .ifPresent(SneakyConsumer.sneaky(InputStream::reset));
    }

    @Override
    public boolean markSupported() {
        return inputStreamSupplier.get()
            .map(InputStream::markSupported)
            .orElse(false);
    }

    @Override
    public long transferTo(OutputStream out) {
        return inputStreamSupplier.get()
            .map(SneakyFunction.sneaky(inputStream -> inputStream.transferTo(out)))
            .orElse(0L);
    }

    @SneakyThrows
    private Optional<Binary> asBinary(Value value) {
        int valueType = value.getType();
        if (valueType == PropertyType.BINARY) {
            Binary binary = value.getBinary();
            return Optional.of(binary);
        } else {
            log.trace("Not a binary type. Node: {}", jcrPathToNodeWithBinaryProperty);
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public byte[] readNBytes(int len) {
        return inputStreamSupplier.get()
            .map(SneakyFunction.sneaky(inputStream -> inputStream.readNBytes(len)))
            .orElse(new byte[0]);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) {
        return inputStreamSupplier.get()
            .map(SneakyFunction.sneaky(inputStream -> inputStream.readNBytes(b, off, len)))
            .orElse(-1);
    }

    @Override
    public long skip(long n) {
        return inputStreamSupplier.get()
            .map(SneakyFunction.sneaky(inputStream -> inputStream.skip(n)))
            .orElse(0L);
    }

    @Override
    public void skipNBytes(long n) {
        inputStreamSupplier.get()
            .ifPresent(SneakyConsumer.sneaky(inputStream -> inputStream.skipNBytes(n)));
    }

    @Override
    public int available() {
        return inputStreamSupplier.get()
            .map(SneakyFunction.sneaky(InputStream::available))
            .orElse(0);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public byte[] readAllBytes() {
        return inputStreamSupplier.get()
            .map(SneakyFunction.sneaky(InputStream::readAllBytes))
            .orElse(new byte[0]);
    }

    @Override
    public int read() {
        return inputStreamSupplier.get()
            .map(SneakyFunction.sneaky(InputStream::read))
            .orElse(-1);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public int read(byte[] b) {
        return inputStreamSupplier.get()
            .map(SneakyFunction.sneaky(inputStream -> inputStream.read(b)))
            .orElse(-1);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public int read(byte[] b, int off, int len) {
        return inputStreamSupplier.get()
            .map(SneakyFunction.sneaky(inputStream -> inputStream.read(b, off, len)))
            .orElse(-1);
    }

    @Override
    public DataSize dataSize() {
        return binarySupplier.get()
            .flatMap(
                binary -> {
                    try {
                        return Optional.of(binary.getSize());
                    } catch (RepositoryException | IllegalStateException exception) {
                        String message = String.format(
                            "Failed to retrieve size of binary for %s", jcrPathToNodeWithBinaryProperty
                        );
                        log.error(message, exception);
                        return Optional.empty();
                    }
                }
            ).map(sizeInBytes -> new DataSize(sizeInBytes, DataUnit.BYTES))
            .orElse(new DataSize(NumberUtils.LONG_ZERO, DataUnit.BYTES));
    }
}
