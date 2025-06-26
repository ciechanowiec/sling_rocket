package eu.ciechanowiec.sling.rocket.google;

import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.Member;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalGroup;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityException;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityRef;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"NullableProblems", "PMD.LooseCoupling"})
@ToString
@Slf4j
class GoogleExternalGroup implements ExternalGroup {

    private final Group group;
    @ToString.Exclude
    private final GoogleDirectory googleDirectory;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    GoogleExternalGroup(Group group, GoogleDirectory googleDirectory) {
        this.group = group;
        this.googleDirectory = googleDirectory;
    }

    @Override
    public Iterable<ExternalIdentityRef> getDeclaredMembers() {
        log.trace("Getting declared members of {}", this);
        String providerName = GoogleIdentityProvider.class.getSimpleName();
        return Optional.ofNullable(group.getEmail())
            .map(googleDirectory::listMembers)
            .orElse(List.of())
            .stream()
            .map(Member::getEmail)
            .map(memberEmail -> new ExternalIdentityRef(memberEmail, providerName))
            .toList();
    }

    @Override
    public ExternalIdentityRef getExternalId() {
        String providerName = GoogleIdentityProvider.class.getSimpleName();
        return Optional.ofNullable(group.getEmail())
            .map(groupEmail -> new ExternalIdentityRef(groupEmail, providerName))
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
        return "google";
    }

    @Override
    public Iterable<ExternalIdentityRef> getDeclaredGroups() throws ExternalIdentityException {
        log.trace("Getting declared groups of {}", this);
        String providerName = GoogleIdentityProvider.class.getSimpleName();
        return Optional.ofNullable(group.getEmail())
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
            "email", Optional.ofNullable(group.getEmail()).orElse(StringUtils.EMPTY),
            "name", Optional.ofNullable(group.getName()).orElse(StringUtils.EMPTY),
            "description", Optional.ofNullable(group.getDescription()).orElse(StringUtils.EMPTY),
            "id", Optional.ofNullable(group.getId()).orElse(StringUtils.EMPTY),
            "kind", Optional.ofNullable(group.getKind()).orElse(StringUtils.EMPTY)
        );
    }
}
