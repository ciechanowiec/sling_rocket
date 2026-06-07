package eu.ciechanowiec.sling.rocket.clamav;

import org.apache.commons.lang3.Strings;

/**
 * Parser of raw replies of a ClamAV daemon (clamd) to {@code INSTREAM} commands.
 */
final class ClamdReply {

    private static final String CLEAN_REPLY = "stream: OK";
    private static final String FOUND_SUFFIX = " FOUND";
    private static final String STREAM_PREFIX = "stream: ";

    private ClamdReply() {
        // No instances allowed
    }

    /**
     * Converts the passed raw reply of a ClamAV daemon (clamd) to an {@code INSTREAM} command into a
     * {@link ScanResult}.
     *
     * @param rawReply raw reply of a ClamAV daemon (clamd) to an {@code INSTREAM} command
     * @return {@link ScanResult} derived from the passed raw reply
     */
    static ScanResult toScanResult(String rawReply) {
        String reply = rawReply.strip();
        if (reply.equals(CLEAN_REPLY)) {
            return new Clean();
        }
        return reply.endsWith(FOUND_SUFFIX)
            ? new Infected(signatureName(reply))
            : new Failed(failureDetails(reply));
    }

    private static String signatureName(String reply) {
        String withoutSuffix = Strings.CS.removeEnd(reply, FOUND_SUFFIX);
        return Strings.CS.removeStart(withoutSuffix, STREAM_PREFIX).strip();
    }

    private static String failureDetails(String reply) {
        return reply.isEmpty() ? "Empty reply from clamd" : reply;
    }
}
