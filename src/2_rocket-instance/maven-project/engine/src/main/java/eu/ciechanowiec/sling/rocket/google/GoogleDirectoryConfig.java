package eu.ciechanowiec.sling.rocket.google;

import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for {@link GoogleDirectory}.
 */
@ObjectClassDefinition
public @interface GoogleDirectoryConfig {

    /**
     * Email of the user to impersonate when accessing the {@link Directory} by a service user.
     * <p>
     * All actions performed by the service user configured via {@link #path$_$to$_$service$_$account$_$key$_$file()}
     * will be performed on behalf of this user via domain-wide delegation.
     * <p>
     * This user must possess administrative privileges in the Google Admin Console that correspond to at least all the
     * scopes defined in {@link #directory$_$scopes()}.
     *
     * @return email of the user to impersonate when accessing the {@link Directory} by a service user
     */
    @AttributeDefinition(
        name = "User to Impersonate - Email",
        description = "Email of the user to impersonate when accessing the Directory by a service user",
        defaultValue = StringUtils.EMPTY,
        type = AttributeType.STRING
    )
    String user$_$to$_$impersonate_email() default StringUtils.EMPTY;

    /**
     * Absolute path in the file system to the JSON key file of the service account that will be used to access the
     * {@link Directory}.
     * <p>
     * The service account associated with this key file must be configured for domain-wide delegation in the Google
     * Admin Console. It must be granted the scopes defined in {@link #directory$_$scopes()} to be able to impersonate
     * the user specified in {@link #user$_$to$_$impersonate_email()}.
     * <p>
     * The file should be attached as the volume of type {@code bind} to the Sling Rocket Docker container in the
     * {@code docker-compose.yml} file, e.g., this way:
     * <pre>
     * - type: bind
     *   source: secrets/GOOGLE_DIRECTORY_SERVICE_ACCOUNT_KEY_FILE.JSON
     *   target: /mnt/secrets/GOOGLE_DIRECTORY_SERVICE_ACCOUNT_KEY_FILE.JSON
     *   read_only: true
     * </pre>
     *
     * If the {@code docker-compose.yml} file is configured as in the above example, the value of this property should
     * be set to {@code /mnt/secrets/GOOGLE_DIRECTORY_SERVICE_ACCOUNT_KEY_FILE.JSON}.
     *
     * @return absolute path in the file system to the JSON key file of the service account that will be used to access
     * the {@link Directory}
     */
    @SuppressWarnings("NewMethodNamingConvention")
    @AttributeDefinition(
        name = "Path to Service Account Key File",
        description = "Absolute path in the file system to the JSON key file of the service "
            + "account that will be used to access the Directory",
        defaultValue = StringUtils.EMPTY,
        type = AttributeType.STRING
    )
    String path$_$to$_$service$_$account$_$key$_$file() default StringUtils.EMPTY;

    /**
     * Available {@link DirectoryScopes} for the {@link Directory}.
     *
     * @return available {@link DirectoryScopes} for the {@link Directory}
     */
    @AttributeDefinition(
        name = "Directory Scopes",
        description = "Directory Scopes for the Directory",
        defaultValue = {
            DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY,
            DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER_READONLY,
            DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY
        },
        type = AttributeType.STRING
    )
    String[] directory$_$scopes() default {
        DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY,
        DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER_READONLY,
        DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY
    };
}
