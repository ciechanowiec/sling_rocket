package eu.ciechanowiec.sling.rocket.google;

import javax.jcr.SimpleCredentials;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleSimpleCredentialsTest {

    @Test
    void testEmailAndIdTokenExtraction() {
        String email = "test@example.com";
        String idToken = "test-id-token";
        SimpleCredentials simpleCredentials = new SimpleCredentials(email, idToken.toCharArray());
        GoogleSimpleCredentials googleSimpleCredentials = new GoogleSimpleCredentials(simpleCredentials);
        assertEquals(email, googleSimpleCredentials.email());
        assertEquals(idToken, googleSimpleCredentials.idToken());
    }

    @Test
    void testToString() {
        String email = "test@example.com";
        String idToken = "test-id-token";
        SimpleCredentials simpleCredentials = new SimpleCredentials(email, idToken.toCharArray());
        GoogleSimpleCredentials googleSimpleCredentials = new GoogleSimpleCredentials(simpleCredentials);
        String toString = googleSimpleCredentials.toString();
        assertTrue(toString.contains(email));
        assertFalse(toString.contains(idToken));
    }
}
