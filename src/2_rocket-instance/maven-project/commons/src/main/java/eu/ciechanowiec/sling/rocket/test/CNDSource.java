package eu.ciechanowiec.sling.rocket.test;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Optional;
import java.util.jar.JarFile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class CNDSource {

    @SneakyThrows
    InputStreamReader get() {
        log.debug("Getting CND source");
        String cndPath = "SLING-INF/notetypes/nodetypes.cnd";
        return currentJarFile().flatMap(
                jar -> Optional.ofNullable(jar.getJarEntry(cndPath))
                    .flatMap(SneakyFunction.sneaky(entry -> Optional.ofNullable(jar.getInputStream(entry))))
            )
            .map(cndIS -> new InputStreamReader(cndIS, StandardCharsets.UTF_8))
            .orElseGet(
                () -> {
                    log.debug("CND not found in JAR, trying to load from the classpath");
                    ClassLoader classLoader = CNDSource.class.getClassLoader();
                    InputStream cndIS = Optional.ofNullable(classLoader.getResourceAsStream(cndPath))
                        .orElseThrow();
                    return new InputStreamReader(cndIS, StandardCharsets.UTF_8);
                }
            );
    }

    @SneakyThrows
    private Optional<JarFile> currentJarFile() {
        ProtectionDomain protectionDomain = CNDSource.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL jarURL = codeSource.getLocation();
        URI jarURI = jarURL.toURI();
        File jarFile = new File(jarURI);
        if (jarFile.isFile()) {
            log.debug("JAR file found: {}", jarFile);
            return Optional.of(new JarFile(jarFile));
        } else {
            log.debug("No JAR file found");
            return Optional.empty();
        }
    }
}
