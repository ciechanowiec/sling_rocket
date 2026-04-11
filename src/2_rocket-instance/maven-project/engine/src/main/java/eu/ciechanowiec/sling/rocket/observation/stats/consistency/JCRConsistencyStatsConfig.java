package eu.ciechanowiec.sling.rocket.observation.stats.consistency;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.nio.file.Path;

/**
 * Configuration for {@link JCRConsistencyStats}.
 */
@SuppressWarnings("WeakerAccess")
@ObjectClassDefinition
public @interface JCRConsistencyStatsConfig {

    /**
     * {@link Path} to the  <a href="https://jackrabbit.apache.org/oak/docs/osgi_config.html">{@code segmentstore}</a>
     * directory backed up by the SR Backuper.
     *
     * @return {@link Path} to the  <a
     * href="https://jackrabbit.apache.org/oak/docs/osgi_config.html">{@code segmentstore}</a> directory backed up by
     * the SR Backuper
     */
    @AttributeDefinition(
        name = "Segment Node Store Directory Backup",
        description = "Path to the segmentstore directory backed up by the SR Backuper",
        type = AttributeType.STRING,
        defaultValue = "/var/rocket-data-dump/backup"
    )
    @SuppressWarnings("squid:S100")
    String backup$_$segmentstore$_$path() default "/var/rocket-data-dump/backup";
}
