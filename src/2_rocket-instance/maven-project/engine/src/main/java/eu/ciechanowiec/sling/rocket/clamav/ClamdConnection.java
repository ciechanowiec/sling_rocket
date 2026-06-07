package eu.ciechanowiec.sling.rocket.clamav;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Single {@link Socket} connection to a ClamAV daemon (clamd) over which exactly one command can be exchanged.
 * <p>
 * The commands are sent in the {@code z}-style of the clamd protocol, i.e. both the commands and the replies are
 * delimited with a {@code NUL} character.
 */
@Slf4j
final class ClamdConnection implements AutoCloseable {

    private static final int CHUNK_SIZE = 65_536;
    private static final int END_OF_STREAM = -1;
    private static final int NUL = 0;
    private static final int STREAM_TERMINATOR = 0;
    private static final String PONG_REPLY = "PONG";
    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PING_COMMAND = "zPING\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] VERSION_COMMAND = "zVERSION\0".getBytes(StandardCharsets.US_ASCII);

    private final Socket socket;

    /**
     * Constructs an instance of this class, connected to the ClamAV daemon (clamd) at the passed host and port.
     *
     * @param host                 host of the ClamAV daemon (clamd) to connect to
     * @param port                 TCP port of the ClamAV daemon (clamd) to connect to
     * @param connectTimeoutMillis maximum time in milliseconds to wait for the connection to be established
     * @param readTimeoutMillis    maximum time in milliseconds to wait for every single read to complete
     * @throws IOException if the connection cannot be established
     */
    ClamdConnection(String host, int port, int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
        this.socket = new Socket();
        try {
            this.socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            this.socket.setSoTimeout(readTimeoutMillis);
        } catch (IOException exception) {
            closeQuietly();
            throw exception;
        }
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException exception) {
            log.debug("Unable to close the socket", exception);
        }
    }

    /**
     * Scans the content provided by the passed {@link InputStream} for viruses via the {@code INSTREAM} command.
     *
     * @param content {@link InputStream} that provides the content that should be scanned
     * @return {@link ScanResult} of scanning the passed content
     */
    ScanResult scan(InputStream content) {
        try {
            return requestScan(content);
        } catch (IOException exception) {
            log.debug("Streaming the scanned content to clamd failed. A pending reply will be read", exception);
            return readReplyQuietly()
                .map(ClamdReply::toScanResult)
                .orElseGet(() -> new Failed("Streaming the scanned content to clamd failed: " + exception));
        }
    }

    /**
     * Checks whether the ClamAV daemon (clamd) is reachable and responsive via the {@code PING} command.
     *
     * @return {@code true} if the ClamAV daemon (clamd) replied with a {@code PONG}; {@code false} otherwise
     * @throws IOException if the command exchange fails
     */
    boolean ping() throws IOException {
        sendCommand(PING_COMMAND);
        return PONG_REPLY.equals(readReply());
    }

    /**
     * Retrieves the version of the ClamAV daemon (clamd) and of its virus definitions database via the {@code VERSION}
     * command.
     *
     * @return version of the ClamAV daemon (clamd) and of its virus definitions database; an empty {@link Optional} is
     * returned if the ClamAV daemon (clamd) didn't report any version
     * @throws IOException if the command exchange fails
     */
    Optional<String> version() throws IOException {
        sendCommand(VERSION_COMMAND);
        return Optional.of(readReply()).filter(reply -> !reply.isEmpty());
    }

    @SuppressWarnings("PMD.CloseResource")
    private ScanResult requestScan(InputStream content) throws IOException {
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        output.write(INSTREAM_COMMAND);
        writeChunks(content, output);
        output.writeInt(STREAM_TERMINATOR);
        output.flush();
        return ClamdReply.toScanResult(readReply());
    }

    private void writeChunks(InputStream content, DataOutputStream output) throws IOException {
        byte[] buffer = new byte[CHUNK_SIZE];
        int numOfReadBytes = content.read(buffer);
        while (numOfReadBytes != END_OF_STREAM) {
            if (numOfReadBytes > 0) {
                output.writeInt(numOfReadBytes);
                output.write(buffer, 0, numOfReadBytes);
            }
            numOfReadBytes = content.read(buffer);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private String readReply() throws IOException {
        InputStream input = socket.getInputStream();
        ByteArrayOutputStream replyBytes = new ByteArrayOutputStream();
        int singleByte = input.read();
        while (singleByte != END_OF_STREAM && singleByte != NUL) {
            replyBytes.write(singleByte);
            singleByte = input.read();
        }
        return replyBytes.toString(StandardCharsets.UTF_8).strip();
    }

    private Optional<String> readReplyQuietly() {
        try {
            return Optional.of(readReply()).filter(value -> !value.isEmpty());
        } catch (IOException exception) {
            log.debug("Unable to read a pending reply from clamd", exception);
            return Optional.empty();
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private void sendCommand(byte[] command) throws IOException {
        OutputStream output = socket.getOutputStream();
        output.write(command);
        output.flush();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
