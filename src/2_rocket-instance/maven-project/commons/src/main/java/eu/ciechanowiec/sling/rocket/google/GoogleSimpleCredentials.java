package eu.ciechanowiec.sling.rocket.google;

import lombok.ToString;

import javax.jcr.SimpleCredentials;

@ToString
class GoogleSimpleCredentials {

    private final String email;
    @ToString.Exclude
    private final String idToken;

    GoogleSimpleCredentials(SimpleCredentials simpleCredentials) {
        email = simpleCredentials.getUserID();
        char[] password = simpleCredentials.getPassword();
        idToken = new String(password);
    }

    String email() {
        return email;
    }

    String idToken() {
        return idToken;
    }
}
