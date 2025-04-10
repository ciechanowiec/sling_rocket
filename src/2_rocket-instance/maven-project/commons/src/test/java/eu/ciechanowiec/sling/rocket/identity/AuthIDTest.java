package eu.ciechanowiec.sling.rocket.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
class AuthIDTest {

    @Test
    void testAuthIDUniversalGet() {
        AuthID authIDUniversal = new AuthIDUniversal("testID");
        assertEquals("testID", authIDUniversal.get());
    }

    @Test
    void testAuthIDUniversalEquals() {
        AuthID authID1 = new AuthIDUniversal("testID");
        AuthID authID2 = new AuthIDUniversal("testID");
        AuthID authID3 = new AuthIDUniversal("differentID");

        assertEquals(authID1, authID2);
        assertNotEquals(authID1, authID3);
        assertNotEquals(null, authID1);
    }

    @Test
    void testAuthIDUniversalHashCode() {
        AuthID authID1 = new AuthIDUniversal("testID");
        AuthID authID2 = new AuthIDUniversal("testID");
        AuthID authID3 = new AuthIDUniversal("differentID");

        assertEquals(authID1.hashCode(), authID2.hashCode());
        assertNotEquals(authID1.hashCode(), authID3.hashCode());
    }

    @Test
    void testAuthIDUniversalCompareTo() {
        AuthID authID1 = new AuthIDUniversal("testID");
        AuthID authID2 = new AuthIDUniversal("testID");
        AuthID authID3 = new AuthIDUniversal("otherID");

        assertEquals(0, authID1.compareTo(authID2));
        assertTrue(authID1.compareTo(authID3) > 0);
        assertTrue(authID3.compareTo(authID1) < 0);
    }

    @Test
    void testAuthIDGroupGet() {
        AuthID authIDGroup = new AuthIDGroup("groupID");
        assertEquals("groupID", authIDGroup.get());
    }

    @Test
    void testAuthIDGroupEquals() {
        AuthID authIDGroup1 = new AuthIDGroup("groupID");
        AuthID authIDGroup2 = new AuthIDGroup("groupID");
        AuthID authIDGroup3 = new AuthIDGroup("differentID");

        assertEquals(authIDGroup1, authIDGroup2);
        assertNotEquals(authIDGroup1, authIDGroup3);
    }

    @Test
    void testAuthIDGroupHashCode() {
        AuthID authIDGroup1 = new AuthIDGroup("groupID");
        AuthID authIDGroup2 = new AuthIDGroup("groupID");
        AuthID authIDGroup3 = new AuthIDGroup("differentID");

        assertEquals(authIDGroup1.hashCode(), authIDGroup2.hashCode());
        assertNotEquals(authIDGroup1.hashCode(), authIDGroup3.hashCode());
    }

    @Test
    void testAuthIDGroupCompareTo() {
        AuthID authIDGroup1 = new AuthIDGroup("groupID");
        AuthID authIDGroup2 = new AuthIDGroup("groupID");
        AuthID authIDGroup3 = new AuthIDGroup("otherID");

        assertEquals(0, authIDGroup1.compareTo(authIDGroup2));
        assertTrue(authIDGroup1.compareTo(authIDGroup3) < 0);
        assertTrue(authIDGroup3.compareTo(authIDGroup1) > 0);
    }

    @Test
    void testAuthIDUserGet() {
        AuthID authIDUser = new AuthIDUser("userID");
        assertEquals("userID", authIDUser.get());
    }

    @Test
    void testAuthIDUserEquals() {
        AuthID authIDUser1 = new AuthIDUser("userID");
        AuthID authIDUser2 = new AuthIDUser("userID");
        AuthID authIDUser3 = new AuthIDUser("differentID");

        assertEquals(authIDUser1, authIDUser2);
        assertNotEquals(authIDUser1, authIDUser3);
    }

    @Test
    void testAuthIDUserHashCode() {
        AuthID authIDUser1 = new AuthIDUser("userID");
        AuthID authIDUser2 = new AuthIDUser("userID");
        AuthID authIDUser3 = new AuthIDUser("differentID");

        assertEquals(authIDUser1.hashCode(), authIDUser2.hashCode());
        assertNotEquals(authIDUser1.hashCode(), authIDUser3.hashCode());
    }

    @Test
    void testAuthIDUserCompareTo() {
        AuthID authIDUser1 = new AuthIDUser("userID");
        AuthID authIDUser2 = new AuthIDUser("userID");
        AuthID authIDUser3 = new AuthIDUser("otherID");

        assertEquals(0, authIDUser1.compareTo(authIDUser2));
        assertTrue(authIDUser1.compareTo(authIDUser3) > 0);
        assertTrue(authIDUser3.compareTo(authIDUser1) < 0);
    }

    @Test
    void testMixedAuths() {
        AuthID authIDUniversal = new AuthIDUniversal("someID");
        AuthID authIDUser = new AuthIDUser("someID");
        AuthID authIDGroup = new AuthIDGroup("someID");

        assertNotEquals(authIDUser, authIDGroup);
        assertNotEquals(authIDUser, authIDUniversal);
        assertNotEquals(authIDGroup, authIDUniversal);
    }
}
