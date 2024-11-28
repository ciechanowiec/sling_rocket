package eu.ciechanowiec.sling.rocket.privilege;

import eu.ciechanowiec.sling.rocket.commons.ResourceAccess;
import eu.ciechanowiec.sling.rocket.commons.UserResourceAccess;
import eu.ciechanowiec.sling.rocket.identity.AuthIDGroup;
import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.identity.SimpleAuthorizable;
import eu.ciechanowiec.sling.rocket.jcr.DefaultProperties;
import eu.ciechanowiec.sling.rocket.jcr.NodeProperties;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrivilegeAdminTest extends TestEnvironment {

    PrivilegeAdminTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @Test
    void testBasicAllowDeny() {
        String path = "/content/rocket";
        context.build().resource(path).commit();
        AuthIDGroup testedGroup = createOrGetGroup(new AuthIDGroup("testedGroup"));
        AuthIDUser userFromGroup = createOrGetUser(new AuthIDUser("userFromGroup"));
        AuthIDUser nonExistentUser = new AuthIDUser("nonExistentUser");
        SimpleAuthorizable simpleAuthorizable = new SimpleAuthorizable(userFromGroup, fullResourceAccess);
        simpleAuthorizable.addToGroup(testedGroup);
        AuthIDUser userUserWithoutGroup = createOrGetUser(new AuthIDUser("userUserWithoutGroup"));
        ResourceAccess userFromGroupRR = new UserResourceAccess(userFromGroup, fullResourceAccess);
        ResourceAccess userUserWithoutGroupRR = new UserResourceAccess(userUserWithoutGroup, fullResourceAccess);
        PrivilegeAdmin privilegeAdmin = new PrivilegeAdmin(fullResourceAccess);
        boolean wasAllowedFirst = privilegeAdmin.allow(
                new TargetJCRPath(path), testedGroup, PrivilegeConstants.JCR_READ
        );
        boolean wasAllowedSecond = privilegeAdmin.allow(
                new TargetJCRPath(path), testedGroup, PrivilegeConstants.JCR_READ
        );
        assertAll(
                () -> assertTrue(wasAllowedFirst),
                () -> assertFalse(wasAllowedSecond),
                () -> assertFalse(privilegeAdmin.allow(
                        new TargetJCRPath(path), nonExistentUser, PrivilegeConstants.JCR_READ)
                ),
                () -> assertNotNull(userFromGroupRR.acquireAccess().getResource(path)),
                () -> assertNull(userUserWithoutGroupRR.acquireAccess().getResource(path))
        );
        boolean wasDeniedFirst = privilegeAdmin.deny(
                new TargetJCRPath(path), testedGroup, PrivilegeConstants.JCR_READ
        );
        boolean wasDeniedSecond = privilegeAdmin.deny(
                new TargetJCRPath(path), testedGroup, PrivilegeConstants.JCR_READ
        );
        assertAll(
                () -> assertTrue(wasDeniedFirst),
                () -> assertFalse(wasDeniedSecond),
                () -> assertNull(userFromGroupRR.acquireAccess().getResource(path))
        );
    }

    @Test
    void testWrite() {
        String propertyName = "somus-namus";
        String propertyValue = "somus-propus";
        String path = "/content/rocket";
        context.build().resource(path, Map.of(propertyName, propertyValue)).commit();
        AuthIDUser testUser = createOrGetUser(new AuthIDUser("testUser"));
        UserResourceAccess userResourceAccess = new UserResourceAccess(testUser, fullResourceAccess);
        NodeProperties nodeProperties = new NodeProperties(new TargetJCRPath(path), userResourceAccess);
        assertTrue(nodeProperties.propertyValue(propertyName, DefaultProperties.STRING_CLASS).isEmpty());
        PrivilegeAdmin privilegeAdmin = new PrivilegeAdmin(fullResourceAccess);
        privilegeAdmin.allow(new TargetJCRPath(path), testUser, PrivilegeConstants.JCR_READ);
        assertEquals(
                propertyValue, nodeProperties.propertyValue(propertyName, DefaultProperties.STRING_CLASS).orElseThrow()
        );
        assertThrows(PersistenceException.class, () -> nodeProperties.setProperty(propertyName, "some-new-value"));
        assertEquals(
                propertyValue, nodeProperties.propertyValue(propertyName, DefaultProperties.STRING_CLASS).orElseThrow()
        );
        privilegeAdmin.deny(new TargetJCRPath(path), testUser, PrivilegeConstants.JCR_READ);
        assertTrue(nodeProperties.propertyValue(propertyName, DefaultProperties.STRING_CLASS).isEmpty());
        privilegeAdmin.allow(new TargetJCRPath(path), testUser, PrivilegeConstants.JCR_READ);
        privilegeAdmin.allow(new TargetJCRPath(path), testUser, PrivilegeConstants.REP_WRITE);
        nodeProperties.setProperty(propertyName, "some-new-value");
        assertEquals(
                "some-new-value", nodeProperties.propertyValue(
                        propertyName, DefaultProperties.STRING_CLASS
                ).orElseThrow()
        );
    }
}
