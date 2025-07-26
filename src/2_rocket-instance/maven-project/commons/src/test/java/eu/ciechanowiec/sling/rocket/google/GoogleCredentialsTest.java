package eu.ciechanowiec.sling.rocket.google;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleCredentialsTest {

    @Test
    void testEmailAndIdTokenExtraction() {
        String email = "test@example.com";
        String idToken = "test-id-token";
        GoogleCredentials googleCredentials = new GoogleCredentials(email, idToken.toCharArray());
        assertEquals(email, googleCredentials.email());
        assertEquals(idToken, googleCredentials.idToken());
    }

    @Test
    void testToString() {
        String email = "test@example.com";
        String idToken = "test-id-token";
        GoogleCredentials googleCredentials = new GoogleCredentials(email, idToken.toCharArray());
        String toString = googleCredentials.toString();
        assertTrue(toString.contains(email));
        assertFalse(toString.contains(idToken));
    }
}
