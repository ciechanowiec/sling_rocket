package eu.ciechanowiec.sling.rocket.clamav;

import eu.ciechanowiec.sling.rocket.asset.Asset;
import eu.ciechanowiec.sling.rocket.asset.AssetFile;

import java.io.InputStream;
import java.util.Optional;

/**
 * Scans content for viruses with an antivirus engine.
 * <p>
 * No method of this interface ever throws: every scan produces a {@link ScanResult}, and infrastructure problems are
 * reported as {@link Failed}. This way the client code is forced to explicitly distinguish content that was actually
 * verified as {@link Clean} from content that simply couldn't be scanned.
 */
public interface VirusScanner {

    /**
     * Scans the content provided by the passed {@link InputStream} for viruses.
     * <p>
     * The passed {@link InputStream} is consumed, but it is <i>not</i> closed by this method: closing it is the
     * responsibility of the caller who created it.
     *
     * @param content {@link InputStream} that provides the content that should be scanned
     * @return {@link ScanResult} of scanning the passed content
     */
    ScanResult scan(InputStream content);

    /**
     * Scans for viruses the {@link AssetFile} of the passed {@link Asset}, provided by {@link Asset#assetFile()}.
     *
     * @param asset {@link Asset} whose {@link AssetFile} should be scanned
     * @return {@link ScanResult} of scanning the {@link AssetFile} of the passed {@link Asset}
     */
    ScanResult scan(Asset asset);

    /**
     * Scans for viruses the content of the passed {@link AssetFile}, retrieved via {@link AssetFile#retrieve()}.
     *
     * @param assetFile {@link AssetFile} whose content should be scanned
     * @return {@link ScanResult} of scanning the content of the passed {@link AssetFile}
     */
    ScanResult scan(AssetFile assetFile);

    /**
     * Checks whether the antivirus engine is reachable and responsive.
     *
     * @return {@code true} if the antivirus engine is reachable and responsive; {@code false} otherwise
     */
    boolean ping();

    /**
     * Returns the version of the antivirus engine and of its virus definitions database, e.g.
     * {@code ClamAV 1.5.2/28023/Sat Jun  6 06:30:12 2026}.
     *
     * @return version of the antivirus engine and of its virus definitions database; an empty {@link Optional} is
     * returned if the antivirus engine is unreachable or didn't report any version
     */
    Optional<String> version();
}
