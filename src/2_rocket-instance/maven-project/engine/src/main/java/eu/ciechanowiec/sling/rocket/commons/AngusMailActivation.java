package eu.ciechanowiec.sling.rocket.commons;

import jakarta.activation.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.smtp.SMTPTransport;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.Map;
import java.util.Optional;

/**
 * Configures the Jakarta Activation Framework to correctly discover Angus Mail data handlers in an OSGi environment.
 * <p>
 * In OSGi containers, components (bundles) are isolated through separate classloaders. The Jakarta Activation API
 * bundle lacks visibility into other bundles and cannot automatically discover the {@link DataContentHandler}
 * implementations residing in the Angus Mail bundle. This isolation prevents the framework from identifying handlers
 * for essential MIME types, causing {@link UnsupportedDataTypeException} during email processing (e.g., for
 * {@code multipart/mixed} content).
 * <p>
 * This component resolves the visibility constraint by registering a custom {@link DataContentHandlerFactory} and a
 * {@link MailcapCommandMap} with the Jakarta Activation Framework. The factory maintains an explicit mapping of MIME
 * types to Angus Mail implementation classes. When a handler is requested, the factory instantiates the required class
 * using the Angus Mail bundle's classloader, bypassing standard OSGi classloading restrictions.
 * <p>
 * The {@link DataContentHandlerFactory} is a JVM-wide singleton maintained by the Jakarta Activation API bundle. Since
 * the API bundle's lifecycle is independent of this component, the factory persists even if this component's bundle is
 * restarted. The component handles the {@link Error} that arises from attempting to re-register the factory to ensure
 * stable activation across bundle lifecycle transitions.
 */
@Component(
    service = AngusMailActivation.class,
    immediate = true
)
@Slf4j
@ServiceDescription("Fixes OSGi classloader isolation for Angus Mail handlers")
public class AngusMailActivation {

    private static final Map<String, String> MIME_TYPES_TO_HANDLER = Map.of(
        "text/plain", "org.eclipse.angus.mail.handlers.text_plain",
        "text/html", "org.eclipse.angus.mail.handlers.text_html",
        "text/xml", "org.eclipse.angus.mail.handlers.text_xml",
        "multipart/*", "org.eclipse.angus.mail.handlers.multipart_mixed",
        "message/rfc822", "org.eclipse.angus.mail.handlers.message_rfc822"
    );
    private static final String WILDCARD_SUFFIX = "/*";

    /**
     * Constructs an instance of this class.
     */
    @Activate
    public AngusMailActivation() {
        installAngusMailHandlers();
    }

    @SuppressWarnings({"IllegalCatch", "MatchXpath"})
    private void installAngusMailHandlers() {
        CommandMap.setDefaultCommandMap(new MailcapCommandMap());
        String dchfSimpleName = DataContentHandlerFactory.class.getSimpleName();
        try {
            DataContentHandlerFactory factory = mimeType -> createHandler(mimeType).orElse(null);
            DataHandler.setDataContentHandlerFactory(factory);
            log.info("Successfully installed {} for Angus Mail", dchfSimpleName);
        } catch (Error error) {
            // DataHandler.setDataContentHandlerFactory throws a java.lang.Error if called twice.
            // Since the factory is a JVM-wide singleton (stored in the jakarta.activation-api bundle),
            // it survives our bundle's restarts. We can safely ignore this on subsequent activations.
            String expectedMessage = "%s already defined".formatted(dchfSimpleName);
            String actualMessage = error.getMessage();
            boolean isAlreadyDefinedError = expectedMessage.equals(actualMessage);
            if (isAlreadyDefinedError) {
                log.info(
                    "{} for Angus Mail is already installed "
                        + "(likely from a previous bundle activation).", dchfSimpleName
                );
            } else {
                throw error;
            }
        }
    }

    @SuppressWarnings({"checkstyle:Regexp", "checkstyle:MatchXpath"})
    private Optional<DataContentHandler> createHandler(String mimeType) {
        return findHandlerClassName(mimeType)
            .flatMap(this::instantiateHandler);
    }

    private Optional<String> findHandlerClassName(String mimeType) {
        return MIME_TYPES_TO_HANDLER.entrySet().stream()
            .filter(entry -> matchesMimeType(entry.getKey(), mimeType))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    private boolean matchesMimeType(String pattern, String mimeType) {
        if (pattern.endsWith(WILDCARD_SUFFIX)) {
            return mimeType.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return mimeType.equals(pattern) || mimeType.startsWith(pattern + ";");
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    private Optional<DataContentHandler> instantiateHandler(String handlerClassName) {
        try {
            ClassLoader angusClassLoader = SMTPTransport.class.getClassLoader();
            Class<?> handlerClass = angusClassLoader.loadClass(handlerClassName);
            return Optional.of((DataContentHandler) handlerClass.getDeclaredConstructor().newInstance());
        } catch (ReflectiveOperationException exception) {
            log.warn(
                "Failed to load {} for: {}",
                DataContentHandler.class.getSimpleName(), handlerClassName, exception
            );
            return Optional.empty();
        }
    }
}
