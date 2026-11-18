package com.ruben.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ruben.backend.notification.config.MailProperties;
import com.ruben.backend.notification.event.InvitationCreatedEvent;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@Tag("I1")
@ExtendWith(MockitoExtension.class)
class InvitationMailListenerUnitTest {

  private static final String FROM = "TripMate <no-reply@tripmate.local>";
  private static final InvitationCreatedEvent EVENT =
      new InvitationCreatedEvent(5L, "friend@example.com", "secret-token", "Ana", "Verano en Roma");

  @Mock private JavaMailSender mailSender;

  private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
  private final Logger logger = (Logger) LoggerFactory.getLogger(InvitationMailListener.class);

  @BeforeEach
  void captureLogs() {
    logs.start();
    logger.addAppender(logs);
  }

  @AfterEach
  void stopCapturingLogs() {
    logger.detachAppender(logs);
  }

  private InvitationMailListener listener(String from) {
    MailProperties properties = new MailProperties(from, "https://tripmate.example");
    return new InvitationMailListener(
        mailSender, new InvitationEmailComposer(properties), properties);
  }

  private void mailSenderCreatesMessages() {
    when(mailSender.createMimeMessage())
        .thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
  }

  private String loggedText() {
    return logs.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
  }

  @Test
  void sendsTheEmailFromTheConfiguredSenderToTheInvitedAddressWithTheLink() throws Exception {
    mailSenderCreatesMessages();

    listener(FROM).onInvitationCreated(EVENT);

    ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(sent.capture());
    MimeMessage message = sent.getValue();
    assertThat(message.getFrom()[0]).hasToString(FROM);
    assertThat(message.getRecipients(Message.RecipientType.TO)).hasSize(1);
    assertThat(message.getRecipients(Message.RecipientType.TO)[0])
        .hasToString("friend@example.com");
    assertThat(message.getSubject()).isEqualTo("Ana te ha invitado a un viaje en TripMate");
    assertThat(message.getContent().toString())
        .contains("«Verano en Roma»")
        .contains("https://tripmate.example/invitations/secret-token");
    // A real transport finalises the headers when it sends; here the mocked sender does not.
    message.saveChanges();
    assertThat(message.getContentType()).startsWith("text/plain").containsIgnoringCase("utf-8");
  }

  @Test
  void logsTheInvitationIdButNeitherTheTokenNorTheAddressWhenSent() {
    mailSenderCreatesMessages();

    listener(FROM).onInvitationCreated(EVENT);

    assertThat(loggedText())
        .contains("invitation 5")
        .doesNotContain("secret-token", "friend@example.com");
  }

  @Test
  void aFailingMailServerIsLoggedAndNeverThrown() {
    mailSenderCreatesMessages();
    doThrow(new MailSendException("Connection refused to friend@example.com secret-token"))
        .when(mailSender)
        .send(any(MimeMessage.class));

    assertThatCode(() -> listener(FROM).onInvitationCreated(EVENT)).doesNotThrowAnyException();

    assertThat(logs.list).hasSize(1);
    assertThat(logs.list.getFirst().getLevel()).isEqualTo(Level.WARN);
    assertThat(loggedText())
        .contains("invitation 5", "MailSendException")
        .doesNotContain("secret-token", "friend@example.com", "Connection refused");
  }

  @Test
  void anInvalidSenderAddressIsLoggedAndNothingIsSent() {
    mailSenderCreatesMessages();

    assertThatCode(() -> listener("not an address <<").onInvitationCreated(EVENT))
        .doesNotThrowAnyException();

    verify(mailSender, never()).send(any(MimeMessage.class));
    assertThat(logs.list).hasSize(1);
    assertThat(logs.list.getFirst().getLevel()).isEqualTo(Level.WARN);
    assertThat(loggedText()).doesNotContain("secret-token", "friend@example.com");
  }

  @Test
  void anErrorWhileCreatingTheMessageIsLoggedAndNeverThrown() {
    when(mailSender.createMimeMessage()).thenThrow(new IllegalStateException("no session"));

    assertThatCode(() -> listener(FROM).onInvitationCreated(EVENT)).doesNotThrowAnyException();

    assertThat(logs.list).hasSize(1);
    assertThat(loggedText()).contains("IllegalStateException");
  }
}
