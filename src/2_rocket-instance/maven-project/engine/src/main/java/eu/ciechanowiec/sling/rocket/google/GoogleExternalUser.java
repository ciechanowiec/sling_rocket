package eu.ciechanowiec.sling.rocket.google;

import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.UserName;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityRef;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"NullableProblems", "PMD.LooseCoupling"})
@ToString
@Slf4j
class GoogleExternalUser implements ExternalUser {

    static final String PN_THUMBNAIL_PHOTO_URL = "thumbnailPhotoUrl";

    private final User user;
    @ToString.Exclude
    private final GoogleDirectory googleDirectory;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    GoogleExternalUser(User user, GoogleDirectory googleDirectory) {
        this.user = user;
        this.googleDirectory = googleDirectory;
    }

    @Override
    public ExternalIdentityRef getExternalId() {
        String providerName = GoogleIdentityProvider.class.getSimpleName();
        return Optional.ofNullable(user.getPrimaryEmail())
            .map(userPrimaryEmail -> new ExternalIdentityRef(userPrimaryEmail, providerName))
            .orElseThrow();
    }

    @Override
    public String getId() {
        return getExternalId().getId();
    }

    @Override
    public String getPrincipalName() {
        return getId();
    }

    @Override
    public String getIntermediatePath() {
        return StringUtils.EMPTY;
    }

    @Override
    public Iterable<ExternalIdentityRef> getDeclaredGroups() {
        log.trace("Getting declared groups of {}", this);
        String providerName = GoogleIdentityProvider.class.getSimpleName();
        return Optional.ofNullable(user.getPrimaryEmail())
            .map(googleDirectory::listGroups)
            .orElse(List.of())
            .stream()
            .map(Group::getEmail)
            .map(groupEmail -> new ExternalIdentityRef(groupEmail, providerName))
            .toList();
    }

    @Override
    public Map<String, ?> getProperties() {
        return Map.of(
            "givenName", Optional.ofNullable(user.getName()).map(UserName::getGivenName).orElse(StringUtils.EMPTY),
            "familyName", Optional.ofNullable(user.getName()).map(UserName::getFamilyName).orElse(StringUtils.EMPTY),
            "primaryEmail", Optional.ofNullable(user.getPrimaryEmail()).orElse(StringUtils.EMPTY),
            "isAdmin", Optional.ofNullable(user.getIsAdmin()).orElse(false),
            "kind", Optional.ofNullable(user.getKind()).orElse(StringUtils.EMPTY),
            PN_THUMBNAIL_PHOTO_URL, Optional.ofNullable(user.getThumbnailPhotoUrl()).orElse(StringUtils.EMPTY)
        );
    }
}
