package eu.ciechanowiec.sling.rocket.network;

class AlreadySentException extends RuntimeException {

    AlreadySentException(Response response) {
        super("Response has already been sent: " + response);
    }

    AlreadySentException(ResponseWithAsset response) {
        super("Response has already been sent: " + response);
    }
}
