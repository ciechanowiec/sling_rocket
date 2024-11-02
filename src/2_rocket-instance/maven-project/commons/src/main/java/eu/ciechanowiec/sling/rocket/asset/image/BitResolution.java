package eu.ciechanowiec.sling.rocket.asset.image;

import lombok.AccessLevel;
import lombok.Getter;

enum BitResolution {

    MIN_64(64),
    AVG_128(128),
    MAX_256(256),
    DEFAULT(256);

    @Getter(AccessLevel.PACKAGE)
    private final int value;

    BitResolution(int value) {
        this.value = value;
    }
}
