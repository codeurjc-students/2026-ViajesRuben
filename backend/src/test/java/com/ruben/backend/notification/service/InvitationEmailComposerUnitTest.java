package com.ruben.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.notification.config.MailProperties;
import com.ruben.backend.notification.event.InvitationCreatedEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("I1")
class InvitationEmailComposerUnitTest {

  private static final String TEMPLATE =
      "{inviterName} invites you\nHello\n{inviterName} invites you to {tripName}: {link}\nBye\n";

  private static MailProperties properties(String publicUrl) {
    return new MailProperties("TripMate <no-reply@tripmate.local>", publicUrl);
  }

  private static InvitationCreatedEvent event(String inviterName, String tripName) {
    return new InvitationCreatedEvent(
        5L, "friend@example.com", "tok_en-123", inviterName, tripName);
  }

  @Test
  void theRealTemplateIsInSpanishAndNamesWhoInvitesTheTripAndTheLink() {
    InvitationEmailComposer composer =
        new InvitationEmailComposer(properties("https://tripmate.example"));

    EmailContent email = composer.compose(event("Ana", "Verano en Roma"));

    assertThat(email.to()).isEqualTo("friend@example.com");
    assertThat(email.subject()).isEqualTo("Ana te ha invitado a un viaje en TripMate");
    assertThat(email.body())
        .contains("Ana te invita a unirte al viaje «Verano en Roma» en TripMate.")
        .contains("https://tripmate.example/invitations/tok_en-123")
        .doesNotContain("{");
  }

  @Test
  void theLinkIsThePublicUrlThenInvitationsThenTheToken() {
    InvitationEmailComposer composer =
        new InvitationEmailComposer(properties("https://tripmate.example"), TEMPLATE);

    assertThat(composer.compose(event("Ana", "Rome")).body())
        .isEqualTo(
            "Hello\nAna invites you to Rome: https://tripmate.example/invitations/tok_en-123\nBye");
  }

  @Test
  void trailingSlashesAndSpacesOfThePublicUrlAreIgnored() {
    InvitationEmailComposer composer =
        new InvitationEmailComposer(properties(" http://localhost:5173// "), TEMPLATE);

    assertThat(composer.compose(event("Ana", "Rome")).body())
        .contains("http://localhost:5173/invitations/tok_en-123");
  }

  @Test
  void theFirstLineIsTheSubjectAndTheRestIsTheBody() {
    InvitationEmailComposer composer =
        new InvitationEmailComposer(properties("https://x.example"), TEMPLATE);

    EmailContent email = composer.compose(event("Ana", "Rome"));

    assertThat(email.subject()).isEqualTo("Ana invites you");
    assertThat(email.body()).startsWith("Hello").doesNotContain("Ana invites you\n");
  }

  @Test
  void windowsLineEndingsInTheTemplateWork() {
    InvitationEmailComposer composer =
        new InvitationEmailComposer(
            properties("https://x.example"), TEMPLATE.replace("\n", "\r\n"));

    EmailContent email = composer.compose(event("Ana", "Rome"));

    assertThat(email.subject()).isEqualTo("Ana invites you");
    assertThat(email.body()).doesNotContain("\r");
  }

  @Test
  void aNameThatLooksLikeAPlaceholderIsWrittenAsIsAndNotReplacedAgain() {
    InvitationEmailComposer composer =
        new InvitationEmailComposer(properties("https://x.example"), TEMPLATE);

    EmailContent email = composer.compose(event("{link}", "{inviterName} $1 \\"));

    assertThat(email.subject()).isEqualTo("{link} invites you");
    assertThat(email.body()).contains("{link} invites you to {inviterName} $1 \\: https://");
  }

  @Test
  void lineBreaksInANameCannotReachTheSubjectHeader() {
    InvitationEmailComposer composer =
        new InvitationEmailComposer(properties("https://x.example"), TEMPLATE);

    EmailContent email = composer.compose(event("Ana\r\nBcc: evil@example.com", "Rome"));

    assertThat(email.subject()).isEqualTo("Ana Bcc: evil@example.com invites you");
    assertThat(email.subject()).doesNotContain("\n", "\r");
  }

  @Test
  void aTemplateWithoutBodyIsRefused() {
    assertThatThrownBy(
            () -> new InvitationEmailComposer(properties("https://x.example"), "only a subject"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
