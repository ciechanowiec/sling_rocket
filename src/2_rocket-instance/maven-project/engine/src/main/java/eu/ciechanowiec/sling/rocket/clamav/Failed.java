package eu.ciechanowiec.sling.rocket.clamav;

/**
 * {@link ScanResult} of a scan that didn't happen or didn't complete, so that nothing can be said about the scanned
 * content. Among others, this is the {@link ScanResult} produced when the antivirus engine is unreachable or when it
 * rejects the scanned content, e.g. due to exceeding the maximum allowed content size.
 *
 * @param details human-readable description of the failure
 */
public record Failed(String details) implements ScanResult {

    @Override
    public String summary() {
        return "FAILED: %s".formatted(details);
    }
}
