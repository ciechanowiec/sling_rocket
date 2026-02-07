package eu.ciechanowiec.sling.rocket.google;

import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Photo described by the {@link com.google.api.services.directory.model.User#getThumbnailPhotoUrl()}.
 */
@Model(
    adaptables = SlingJakartaHttpServletRequest.class
)
@Slf4j
public class GoogleUserPhoto {

    private final Supplier<Optional<URL>> url;

    /**
     * Constructs an instance of this class.
     *
     * @param request {@link SlingJakartaHttpServletRequest} from the {@link User} whose photo should be described
     */
    @SuppressWarnings("PMD.CloseResource")
    @Inject
    @SneakyThrows
    public GoogleUserPhoto(
        @Self
        SlingJakartaHttpServletRequest request
    ) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        url = () -> Optional.ofNullable(resourceResolver.adaptTo(User.class))
            .map(SneakyFunction.sneaky(User::getPath))
            .map("%s/profile"::formatted)
            .map(TargetJCRPath::new)
            .map(targetJCRPath -> new NodeProperties(targetJCRPath, resourceResolver))
            .flatMap(
                nodeProperties -> nodeProperties.propertyValue(
                    GoogleExternalUser.PN_THUMBNAIL_PHOTO_URL, String.class)
            ).flatMap(
                thumbnailPhotoUrl -> {
                    try {
                        log.trace("Parsing thumbnail photo URL: {}", thumbnailPhotoUrl);
                        return Optional.of(URI.create(thumbnailPhotoUrl).toURL());
                    } catch (IllegalArgumentException | MalformedURLException exception) {
                        log.error("Could not parse thumbnail photo URL: {}", thumbnailPhotoUrl, exception);
                        return Optional.empty();
                    }
                }
            );
    }

    /**
     * Returns the {@link URL} of the photo.
     *
     * @return {@link Optional} containing the {@link URL} if the photo is available; an empty {@link Optional} is
     * returned otherwise
     */
    @SuppressWarnings("WeakerAccess")
    public Optional<URL> url() {
        return url.get();
    }
}
