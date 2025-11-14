package eu.ciechanowiec.sling.rocket.jcr.query;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;

import javax.jcr.query.Query;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Interception of {@link Query} logs.
 */
@Slf4j
@Component(
    service = {TurboFilter.class, QueryLogsInterception.class},
    immediate = true
)
@ServiceDescription("Interception of JCR query logs")
public class QueryLogsInterception extends TurboFilter {

    static final String INTERCEPTION_KEY_NAME = "interceptionKey";

    private final List<String> loggersToIntercept;
    private final Layout<ILoggingEvent> layout;
    private final int messageCountLimit;
    private final Map<String, List<ILoggingEvent>> savedILoggingEvents;

    /**
     * Constructs an instance of this class.
     */
    @Activate
    @SuppressWarnings({"MagicNumber", "PMD.ConstructorCallsOverridableMethod"})
    public QueryLogsInterception() {
        this.loggersToIntercept = List.of(
            "org.apache.jackrabbit.oak.query",
            "org.apache.jackrabbit.oak.plugins.index"
        );
        this.layout = createLayout();
        this.messageCountLimit = 500;
        this.savedILoggingEvents = new ConcurrentHashMap<>();
        setName(QueryLogsInterception.class.getSimpleName());
        start();
    }

    @Override
    @SuppressWarnings({"ParameterNumber", "Regexp", "PMD.ExcessiveParameterList", "PMD.ShortVariable"})
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        String interceptionKey = MDC.get(INTERCEPTION_KEY_NAME);
        if (interceptionKey != null && acceptLogger(logger)) {
            ILoggingEvent iLoggingEvent = new LoggingEvent(
                Logger.FQCN, logger, level, format, t, params
            );
            saveILoggingEvent(interceptionKey, iLoggingEvent);
        }
        return FilterReply.NEUTRAL;
    }

    List<String> savedILoggingEvents(String interceptionKey) {
        List<String> savedILoggingEventsAsStrings = Optional.ofNullable(savedILoggingEvents.get(interceptionKey))
            .orElse(List.of())
            .stream()
            .map(layout::doLayout)
            .filter(line -> !line.equals("null\n"))
            .toList();
        log.debug(
            "For interception key '{}' these log lines were intercepted: {}",
            interceptionKey, savedILoggingEventsAsStrings
        );
        return savedILoggingEventsAsStrings;
    }

    void stopInterception(String interceptionKey) {
        savedILoggingEvents.remove(interceptionKey);
    }

    private void saveILoggingEvent(String interceptionKey, ILoggingEvent iLoggingEvent) {
        List<ILoggingEvent> iLoggingEvents = savedILoggingEvents.computeIfAbsent(
            interceptionKey, interceptionKeyToPut -> new CopyOnWriteArrayList<>()
        );
        int amountOfSavedILoggingEvents = iLoggingEvents.size();
        if (amountOfSavedILoggingEvents < messageCountLimit) {
            iLoggingEvents.add(iLoggingEvent);
        }
    }

    private boolean acceptLogger(org.slf4j.Logger logger) {
        String loggerName = logger.getName();
        return loggersToIntercept.stream().anyMatch(loggerName::startsWith);
    }

    private Layout<ILoggingEvent> createLayout() {
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setPattern("%msg%n");
        patternLayout.setOutputPatternAsHeader(false);
        Context loggerContext = (Context) LoggerFactory.getILoggerFactory();
        patternLayout.setContext(loggerContext);
        patternLayout.start();
        return patternLayout;
    }
}
