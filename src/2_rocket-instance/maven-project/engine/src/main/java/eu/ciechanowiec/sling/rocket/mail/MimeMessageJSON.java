package eu.ciechanowiec.sling.rocket.mail;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JSON representation of essential properties of a {@link MimeMessage}.
 */
@Slf4j
@SuppressWarnings({"squid:S1192", "WeakerAccess", "PMD.CognitiveComplexity"})
public class MimeMessageJSON implements JSON {

    private final MimeMessage mimeMessage;

    /**
     * Constructs an instance of this class.
     *
     * @param mimeMessage {@link MimeMessage} that will be represented by this {@link MimeMessageJSON}
     */
    public MimeMessageJSON(MimeMessage mimeMessage) {
        this.mimeMessage = mimeMessage;
    }

    /**
     * Returns the {@link Optional} containing the {@link MimeMessage#getSubject()}. If the subject cannot be extracted
     * an empty {@link Optional} is returned.
     *
     * @return {@link Optional} containing the {@link MimeMessage#getSubject()}; if the subject cannot be extracted an
     * empty {@link Optional} is returned
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public Optional<String> subject() {
        try {
            return Optional.ofNullable(mimeMessage.getSubject());
        } catch (MessagingException exception) {
            log.debug("Failed to extract subject from {}", mimeMessage, exception);
            return Optional.empty();
        }
    }

    /**
     * Returns the {@link List} containing the {@link MimeMessage#getFrom()}. If the sender addresses cannot be
     * extracted an empty {@link List} is returned.
     *
     * @return {@link List} containing the {@link MimeMessage#getFrom()}; if the sender addresses cannot be extracted an
     * empty {@link List} is returned
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> from() {
        try {
            return Optional.ofNullable(mimeMessage.getFrom())
                .map(this::convertToString)
                .orElse(List.of());
        } catch (MessagingException exception) {
            log.debug("Failed to extract sender from {}", mimeMessage, exception);
            return Collections.emptyList();
        }
    }

    /**
     * Returns the {@link List} containing the {@link Message.RecipientType#TO} recipient addresses from
     * {@link MimeMessage#getRecipients(Message.RecipientType)}. If the recipient addresses cannot be extracted an empty
     * {@link List} is returned.
     *
     * @return {@link List} containing the {@link Message.RecipientType#TO} recipient addresses from
     * {@link MimeMessage#getRecipients(Message.RecipientType)}; if the recipient addresses cannot be extracted an empty
     * {@link List} is returned
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> to() {
        try {
            return Optional.ofNullable(mimeMessage.getRecipients(Message.RecipientType.TO))
                .map(this::convertToString)
                .orElse(List.of());
        } catch (MessagingException exception) {
            log.debug("Failed to extract '{}' recipients from {}", Message.RecipientType.TO, mimeMessage, exception);
            return Collections.emptyList();
        }
    }

    /**
     * Returns the {@link List} containing the {@link Message.RecipientType#CC} recipient addresses from
     * {@link MimeMessage#getRecipients(Message.RecipientType)}. If the recipient addresses cannot be extracted an empty
     * {@link List} is returned.
     *
     * @return {@link List} containing the {@link Message.RecipientType#CC} recipient addresses from
     * {@link MimeMessage#getRecipients(Message.RecipientType)}; if the recipient addresses cannot be extracted an empty
     * {@link List} is returned
     */
    @SuppressWarnings("QuestionableName")
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> cc() {
        try {
            return Optional.ofNullable(mimeMessage.getRecipients(Message.RecipientType.CC))
                .map(this::convertToString)
                .orElse(List.of());
        } catch (MessagingException exception) {
            log.trace("Failed to extract '{}' recipients from {}", Message.RecipientType.CC, mimeMessage, exception);
            return Collections.emptyList();
        }
    }

    /**
     * Returns the {@link List} containing the {@link Message.RecipientType#BCC} recipient addresses from
     * {@link MimeMessage#getRecipients(Message.RecipientType)}. If the recipient addresses cannot be extracted an empty
     * {@link List} is returned.
     *
     * @return {@link List} containing the {@link Message.RecipientType#BCC} recipient addresses from
     * {@link MimeMessage#getRecipients(Message.RecipientType)}; if the recipient addresses cannot be extracted an empty
     * {@link List} is returned
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> bcc() {
        try {
            return Optional.ofNullable(mimeMessage.getRecipients(Message.RecipientType.BCC))
                .map(this::convertToString)
                .orElse(List.of());
        } catch (MessagingException exception) {
            log.trace("Failed to extract '{}' recipients from {}", Message.RecipientType.BCC, mimeMessage, exception);
            return Collections.emptyList();
        }
    }

    /**
     * Returns the {@link List} containing the reply-to addresses from {@link MimeMessage#getReplyTo()}. If the reply-to
     * addresses cannot be extracted an empty {@link List} is returned.
     *
     * @return {@link List} containing the reply-to addresses from {@link MimeMessage#getReplyTo()}; if the reply-to
     * addresses cannot be extracted an empty {@link List} is returned
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> replyTo() {
        try {
            return Optional.ofNullable(mimeMessage.getReplyTo())
                .map(this::convertToString)
                .orElse(List.of());
        } catch (MessagingException exception) {
            log.warn("Failed to extract reply-to addresses from {}", mimeMessage, exception);
            return Collections.emptyList();
        }
    }

    /**
     * Returns the {@link Optional} containing the {@link MimeMessage#getSentDate()} of the message. If the sent date
     * cannot be extracted an empty {@link Optional} is returned.
     *
     * @return {@link Optional} containing the {@link MimeMessage#getSentDate()} of the message; if the sent date cannot
     * be extracted an empty {@link Optional} is returned
     */
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public Optional<LocalDateTime> sentDate() {
        try {
            return Optional.ofNullable(mimeMessage.getSentDate())
                .map(Date::toInstant)
                .map(instant -> instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
        } catch (MessagingException exception) {
            log.warn("Failed to extract sent date from {}", mimeMessage, exception);
            return Optional.empty();
        }
    }

    /**
     * Returns the {@link Optional} containing the {@link MimeMessage#getContent()}. If the content cannot be extracted
     * an empty {@link Optional} is returned.
     *
     * @return {@link Optional} containing the {@link MimeMessage#getContent()}; if the content cannot be extracted an
     * empty {@link Optional} is returned
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    public Optional<String> content() {
        try {
            Object content = mimeMessage.getContent();
            return Optional.of(contentToString(content));
        } catch (MessagingException | IOException exception) {
            log.warn("Failed to extract content from MimeMessage", exception);
            return Optional.empty();
        }
    }

    @SuppressWarnings({"OverlyNestedMethod", "ReturnCount", "CognitiveComplexity"})
    private String contentToString(Object content) throws MessagingException {
        return switch (content) {
            case String contentString -> contentString;
            case Multipart contentMultipart -> {
                int multipartCount = contentMultipart.getCount();
                yield IntStream.range(0, multipartCount)
                    .mapToObj(
                        partIndex -> {
                            try {
                                BodyPart bodyPart = contentMultipart.getBodyPart(partIndex);
                                String disposition = bodyPart.getDisposition();
                                if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
                                    return StringUtils.EMPTY;
                                }
                                Object bodyPartContent = bodyPart.getContent();
                                return contentToString(bodyPartContent);
                            } catch (MessagingException | IOException exception) {
                                log.warn("Failed to extract content from body part", exception);
                                return StringUtils.EMPTY;
                            }
                        }
                    )
                    .collect(Collectors.joining());
            }
            case null -> StringUtils.EMPTY;
            default -> content.toString();
        };
    }

    private List<String> convertToString(Address... addresses) {
        return Stream.of(addresses)
            .map(Address::toString)
            .toList();
    }

    /**
     * Returns the {@link Map} containing the headers from {@link MimeMessage#getAllHeaders()}. If the headers cannot be
     * extracted an empty {@link Map} is returned.
     *
     * @return {@link Map} containing the headers from {@link MimeMessage#getAllHeaders()}; if the headers cannot be
     * extracted an empty {@link Map} is returned
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, List<String>> headers() {
        try {
            Enumeration<Header> allHeaders = mimeMessage.getAllHeaders();
            return Collections.unmodifiableMap(
                Collections.list(allHeaders)
                    .stream()
                    .collect(
                        Collectors.groupingBy(
                            Header::getName,
                            ConcurrentHashMap::new,
                            Collectors.mapping(Header::getValue, Collectors.toList())
                        )
                    )
            );
        } catch (MessagingException exception) {
            log.warn("Failed to extract headers from {}", mimeMessage, exception);
            return Map.of();
        }
    }

    @Override
    public String toString() {
        return asJSON();
    }

    @Override
    @SneakyThrows
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());
        return objectMapper.writeValueAsString(this);
    }
}
