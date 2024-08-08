package eu.ciechanowiec.sling.rocket.jcr.path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TargetJCRPathTest {

    @Test
    void mustThrowForInvalidPath() {
        TargetJCRPath targetJCRPath = new TargetJCRPath("/invalid/");
        assertThrows(InvalidJCRPathException.class, targetJCRPath::get);
    }
}
