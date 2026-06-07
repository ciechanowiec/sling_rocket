package eu.ciechanowiec.sling.rocket.clamav;

/**
 * {@link ScanResult} of a completed scan that found no threats in the scanned content.
 */
public record Clean() implements ScanResult {

    @Override
    public String summary() {
        return "CLEAN";
    }
}
