package com.ruben.backend.notification.service;

import com.ruben.backend.notification.config.MailProperties;
import com.ruben.backend.notification.event.InvitationCreatedEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Writes the invitation email: who invites, which trip and the link to answer it, `{publicUrl}
 * /invitations/{token}`. The Spanish text is in the resource `mail/invitation.txt`: its first line
 * is the subject and the rest the body, with the placeholders `{inviterName}`, `{tripName}` and
 * `{link}`. It only builds text, so it is tested without any mail server.
 */
@Component
public class InvitationEmailComposer {

  private static final String TEMPLATE = "mail/invitation.txt";
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{(inviterName|tripName|link)}");

  private final String subjectTemplate;
  private final String bodyTemplate;
  private final String publicUrl;

  @Autowired
  public InvitationEmailComposer(MailProperties properties) {
    this(properties, readTemplate());
  }

  InvitationEmailComposer(MailProperties properties, String template) {
    String text = template.replace("\r\n", "\n");
    int lineEnd = text.indexOf('\n');
    if (lineEnd < 0) {
      throw new IllegalArgumentException("The template needs a subject line and a body");
    }
    this.subjectTemplate = text.substring(0, lineEnd).strip();
    this.bodyTemplate = text.substring(lineEnd + 1).strip();
    this.publicUrl = properties.publicUrl().strip().replaceAll("/+$", "");
  }

  public EmailContent compose(InvitationCreatedEvent event) {
    Map<String, String> values =
        Map.of(
            "inviterName", plain(event.inviterName()),
            "tripName", plain(event.tripName()),
            "link", publicUrl + "/invitations/" + event.token());
    return new EmailContent(
        event.invitedEmail(), fill(subjectTemplate, values), fill(bodyTemplate, values));
  }

  /**
   * One pass over the template, so a name that looks like a placeholder is never replaced again.
   */
  private static String fill(String template, Map<String, String> values) {
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(result, Matcher.quoteReplacement(values.get(matcher.group(1))));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /** Names are typed by users: line breaks and other control characters must not reach a header. */
  private static String plain(String text) {
    return text.replaceAll("\\p{Cntrl}+", " ").strip();
  }

  private static String readTemplate() {
    try (var input = new ClassPathResource(TEMPLATE).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the email template " + TEMPLATE, e);
    }
  }
}
