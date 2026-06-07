package eu.ciechanowiec.sling.rocket.clamav;

/**
 * Result of scanning a piece of content for viruses by a {@link VirusScanner}.
 * <p>
 * The result is a strict tri-state: {@link Clean}, {@link Infected} or {@link Failed}. Every client of a
 * {@link VirusScanner} must handle all three cases explicitly, e.g. with a pattern-matching {@code switch}, so that
 * {@link Failed} (the scan didn't happen or didn't complete) is never confused with {@link Clean} (the scan completed
 * and found no threats):
 * <pre>{@code
 * switch (virusScanner.scan(content)) {
 *     case Clean clean -> allow();
 *     case Infected infected -> reject(infected.signatureName());
 *     case Failed failed -> handleScannerFailure(failed.details());
 * }
 * }</pre>
 */
public sealed interface ScanResult permits Clean, Infected, Failed {

    /**
     * Returns a short human-readable summary of this {@link ScanResult} suitable for logging.
     *
     * @return short human-readable summary of this {@link ScanResult} suitable for logging
     */
    String summary();
}
