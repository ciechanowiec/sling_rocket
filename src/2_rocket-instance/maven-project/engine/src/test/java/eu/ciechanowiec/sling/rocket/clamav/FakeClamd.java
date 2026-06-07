package eu.ciechanowiec.sling.rocket.clamav;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Hermetic in-process fake of a ClamAV daemon (clamd) for tests. Speaks the {@code z}-style clamd protocol
 * over a {@link ServerSocket} bound to a dynamic port.
 */
@Slf4j
@SuppressWarnings("PMD.CloseResource")
final class FakeClamd implements AutoCloseable {

    /**
     * Reply behavior of this {@link FakeClamd}.
     */
    enum Mode {

        /**
         * {@code INSTREAM} content containing the EICAR test string is reported as infected; other content as clean.
         * {@code PING} and {@code VERSION} are answered normally.
         */
        EICAR_AUTO,

        /**
         * Every {@code INSTREAM} is answered with the size limit error. {@code PING} and {@code VERSION} are
         * answered normally.
         */
        SIZE_LIMIT,

        /**
         * The connection is reset after the first received {@code INSTREAM} chunk. {@code PING} and {@code VERSION}
         * are answered normally.
         */
        RESET_MIDSTREAM,

        /**
         * Every command is answered with an unparseable reply.
         */
        GARBAGE,

        /**
         * Every command is answered with no reply at all (graceful connection close).
         */
        NO_REPLY
    }

    static final String EICAR
        = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
    static final String EICAR_SIGNATURE_NAME = "Eicar-Test-Signature";
    private static final String GARBAGE_REPLY = "BLAH-BLAH";
    private static final String VERSION_REPLY = "ClamAV 1.5.2/28023/Test";
    private static final int STREAM_TERMINATOR = 0;
    private static final int MAX_CHUNK_SIZE = 65_536;

    private final ServerSocket serverSocket;
    private final Mode mode;

    @SneakyThrows
    FakeClamd(Mode mode) {
        this.mode = mode;
        this.serverSocket = new ServerSocket(0);
        Thread acceptLoop = new Thread(this::acceptLoop, "fake-clamd");
        acceptLoop.setDaemon(true);
        acceptLoop.start();
        log.info("Started a fake clamd on port {} in mode {}", port(), mode);
    }

    int port() {
        return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            acceptSingleConnection();
        }
    }

    private void acceptSingleConnection() {
        try (Socket socket = serverSocket.accept()) {
            handle(socket);
        } catch (IOException exception) {
            log.debug("Fake clamd connection ended", exception);
        }
    }

    private void handle(Socket socket) throws IOException {
        String command = readUntilNul(socket.getInputStream());
        log.info("Fake clamd received command: '{}'", command);
        switch (command) {
            case "zPING" -> replySimple(socket, "PONG");
            case "zVERSION" -> replySimple(socket, VERSION_REPLY);
            case "zINSTREAM" -> handleInstream(socket);
            default -> reply(socket, GARBAGE_REPLY);
        }
    }

    private void replySimple(Socket socket, String normalReply) throws IOException {
        switch (mode) {
            case GARBAGE -> reply(socket, GARBAGE_REPLY);
            case NO_REPLY -> log.info("Fake clamd deliberately not replying");
            default -> reply(socket, normalReply);
        }
    }

    private void handleInstream(Socket socket) throws IOException {
        if (mode == Mode.RESET_MIDSTREAM) {
            resetAfterFirstChunk(socket);
            return;
        }
        byte[] content = readAllChunks(socket.getInputStream());
        switch (mode) {
            case SIZE_LIMIT -> reply(socket, "INSTREAM size limit exceeded. ERROR");
            case GARBAGE -> reply(socket, GARBAGE_REPLY);
            case NO_REPLY -> log.info("Fake clamd deliberately not replying");
            default -> replyForContent(socket, content);
        }
    }

    private void replyForContent(Socket socket, byte[] content) throws IOException {
        String contentAsString = new String(content, StandardCharsets.US_ASCII);
        if (contentAsString.contains(EICAR)) {
            reply(socket, "stream: %s FOUND".formatted(EICAR_SIGNATURE_NAME));
        } else {
            reply(socket, "stream: OK");
        }
    }

    private void resetAfterFirstChunk(Socket socket) throws IOException {
        DataInputStream input = new DataInputStream(socket.getInputStream());
        int firstChunkLength = input.readInt();
        input.readFully(new byte[firstChunkLength]);
        log.info("Fake clamd read the first chunk of {} bytes and now resets the connection", firstChunkLength);
        socket.setSoLinger(true, 0);
    }

    private byte[] readAllChunks(InputStream rawInput) throws IOException {
        DataInputStream input = new DataInputStream(rawInput);
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        byte[] chunkBuffer = new byte[MAX_CHUNK_SIZE];
        int chunkLength = input.readInt();
        while (chunkLength != STREAM_TERMINATOR) {
            input.readFully(chunkBuffer, 0, chunkLength);
            content.write(chunkBuffer, 0, chunkLength);
            chunkLength = input.readInt();
        }
        return content.toByteArray();
    }

    private void reply(Socket socket, String replyText) throws IOException {
        OutputStream output = socket.getOutputStream();
        output.write((replyText + '\0').getBytes(StandardCharsets.US_ASCII));
        output.flush();
    }

    private String readUntilNul(InputStream input) throws IOException {
        ByteArrayOutputStream commandBytes = new ByteArrayOutputStream();
        int singleByte = input.read();
        while (singleByte > 0) {
            commandBytes.write(singleByte);
            singleByte = input.read();
        }
        return commandBytes.toString(StandardCharsets.US_ASCII);
    }

    @Override
    @SneakyThrows
    public void close() {
        serverSocket.close();
    }
}
