package com.ruben.backend.notification.service;

import com.ruben.backend.notification.config.MailProperties;
import com.ruben.backend.notification.event.InvitationCreatedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the invitation email once the invitation is committed, in the background. A rolled back
 * invitation sends nothing, and a failed email (SMTP down, refused address) only leaves a log line:
 * the invitation stays pending and the email is not retried. The log has the invitation id and the
 * kind of error, never the token or the address.
 */
@Component
public class InvitationMailListener {

  private static final Logger log = LoggerFactory.getLogger(InvitationMailListener.class);

  private final JavaMailSender mailSender;
  private final InvitationEmailComposer composer;
  private final MailProperties properties;

  public InvitationMailListener(
      JavaMailSender mailSender, InvitationEmailComposer composer, MailProperties properties) {
    this.mailSender = mailSender;
    this.composer = composer;
    this.properties = properties;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onInvitationCreated(InvitationCreatedEvent event) {
    try {
      EmailContent email = composer.compose(event);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(properties.from());
      helper.setTo(email.to());
      helper.setSubject(email.subject());
      helper.setText(email.body(), false);
      mailSender.send(message);
      log.info("Email of invitation {} sent", event.invitationId());
    } catch (MessagingException | RuntimeException e) {
      log.warn(
          "Email of invitation {} could not be sent ({})",
          event.invitationId(),
          e.getClass().getSimpleName());
    }
  }
}
