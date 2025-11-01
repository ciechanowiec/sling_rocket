package eu.ciechanowiec.sling.rocket.google;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.identity.WithUserManager;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.SimpleNode;
import eu.ciechanowiec.sling.rocket.jcr.path.JCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.privilege.PrivilegeAdmin;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingJakartaHttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GoogleUserPhotoTest extends TestEnvironment {

    GoogleUserPhotoTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @SneakyThrows
    @Test
    @SuppressWarnings("PMD.CloseResource")
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_INFERRED")
    void mustReturnURL() {
        AuthIDUser authIDUser = createOrGetUser(new AuthIDUser("some-user"));
        UserManager userManager = new WithUserManager(context.resourceResolver()).get();
        User user = Optional.ofNullable((User) userManager.getAuthorizable(authIDUser.get())).orElseThrow();
        JCRPath userPath = new TargetJCRPath(user.getPath());
        new PrivilegeAdmin(fullResourceAccess).allow(userPath, authIDUser, PrivilegeConstants.JCR_READ);
        JCRPath profilePath = new TargetJCRPath(new ParentJCRPath(userPath), "profile");
        new SimpleNode(profilePath, fullResourceAccess).ensureNodeExists();
        NodeProperties nodeProperties = new NodeProperties(profilePath, fullResourceAccess);
        String expected = "https://lh3.googleusercontent.com/a-/ALV-UjWM6eBn5If4gXe21ndaSpXVu4ucOuMQyzZNW02A=s96-c";
        nodeProperties.setProperty(GoogleExternalUser.PN_THUMBNAIL_PHOTO_URL, expected);
        MockSlingJakartaHttpServletRequest request = spy(context.jakartaRequest());
        ResourceResolver rrForUser = spy(getRRForUser(authIDUser));
        when(request.getResourceResolver()).thenReturn(rrForUser);
        when(rrForUser.adaptTo(User.class)).thenReturn(user);
        GoogleUserPhoto googleUserPhoto = Objects.requireNonNull(request.adaptTo(GoogleUserPhoto.class));
        assertEquals(expected, googleUserPhoto.url().orElseThrow().toString());
    }
}
