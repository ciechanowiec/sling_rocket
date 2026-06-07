package eu.ciechanowiec.sling.rocket.clamav;

/**
 * {@link ScanResult} of a completed scan that detected a threat in the scanned content.
 *
 * @param signatureName name of the antivirus signature that matched the scanned content, e.g.
 *                      {@code Eicar-Test-Signature}
 */
public record Infected(String signatureName) implements ScanResult {

    @Override
    public String summary() {
        return "INFECTED: %s".formatted(signatureName);
    }
}
