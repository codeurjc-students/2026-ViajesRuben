package com.ruben.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.invitation.dto.InvitationDTO;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.repository.InvitationRepository;
import com.ruben.backend.invitation.service.InvitationService;
import com.ruben.backend.trip.dto.TripRequest;
import com.ruben.backend.trip.service.TripService;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.service.AdminAccountInitializer;
import com.ruben.backend.user.service.UserService;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The invitation email against a real SMTP server (GreenMail, on the port of the test properties):
 * it is sent after the commit with the right link, never for an invitation that was refused or
 * rolled back, and a mail server that is down does not undo the invitation.
 */
@Tag("I1")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InvitationMailIntegrationTest {

  /** Time to wait to be sure that no email arrives: the send itself takes milliseconds. */
  private static final long NO_EMAIL_WAIT_MILLIS = 1500;

  @RegisterExtension
  static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

  @Autowired private InvitationService invitationService;
  @Autowired private TripService tripService;
  @Autowired private UserService userService;
  @Autowired private InvitationRepository invitationRepository;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private AdminAccountInitializer adminAccountInitializer;
  @Autowired private TransactionTemplate transactions;

  private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
  private final Logger listenerLog = (Logger) LoggerFactory.getLogger(InvitationMailListener.class);

  private UserDTO ana;
  private UserDTO bob;
  private long tripId;

  @BeforeEach
  void setUp() {
    emptyInbox();
    logs.start();
    listenerLog.addAppender(logs);
    databaseCleaner.clear();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    ana = userService.register(new RegisterRequest("Ana", "ana@example.com", "secret-pass"));
    bob = userService.register(new RegisterRequest("Bob", "bob@example.com", "secret-pass"));
    logInAs(ana);
    LocalDate start = LocalDate.of(2027, 7, 10);
    tripId =
        tripService
            .create(new TripRequest("Verano en Roma", "Rome", start, start.plusDays(4), null, null))
            .id();
  }

  @AfterEach
  void tearDown() {
    listenerLog.detachAppender(logs);
    SecurityContextHolder.clearContext();
  }

  /** Makes the user the one of the current request, as the JWT filter would. */
  private void logInAs(UserDTO user) {
    TripMateUserDetails details = userDetailsService.loadUserById(user.id());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));
  }

  private InvitationDTO invite(String email) {
    return invitationService.create(tripId, new InvitationRequest(email));
  }

  private static void emptyInbox() {
    try {
      GREEN_MAIL.purgeEmailFromAllMailboxes();
    } catch (FolderException e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean emailArrives() {
    return GREEN_MAIL.waitForIncomingEmail(NO_EMAIL_WAIT_MILLIS, 1);
  }

  @Test
  void anEmailIsSentAfterTheCommitWithWhoInvitesTheTripAndTheLinkOfTheToken() throws Exception {
    InvitationDTO invitation = invite("Friend@Example.com");

    assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();

    MimeMessage email = GREEN_MAIL.getReceivedMessages()[0];
    String token = invitationRepository.findById(invitation.id()).orElseThrow().getToken();
    assertThat(email.getRecipients(Message.RecipientType.TO)).hasSize(1);
    assertThat(email.getRecipients(Message.RecipientType.TO)[0]).hasToString("friend@example.com");
    assertThat(email.getFrom()[0]).hasToString("TripMate Test <no-reply@test.tripmate.com>");
    assertThat(email.getSubject()).isEqualTo("Ana te ha invitado a un viaje en TripMate");
    assertThat(email.getContent().toString())
        .contains("Ana te invita a unirte al viaje «Verano en Roma» en TripMate.")
        .contains("https://tripmate.test/invitations/" + token);
    assertThat(logs.list)
        .extracting(ILoggingEvent::getFormattedMessage)
        .allSatisfy(message -> assertThat(message).doesNotContain(token, "friend@example.com"));
  }

  @Test
  void everyInvitationGetsItsOwnEmailWithItsOwnLink() throws Exception {
    invite("one@example.com");
    invite("two@example.com");

    assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 2)).isTrue();

    StringBuilder bodies = new StringBuilder();
    for (MimeMessage email : GREEN_MAIL.getReceivedMessages()) {
      bodies.append(email.getContent());
    }
    assertThat(invitationRepository.findAll())
        .hasSize(2)
        .allSatisfy(
            invitation ->
                assertThat(bodies.toString()).contains("/invitations/" + invitation.getToken()));
  }

  @Test
  void noEmailIsSentWhenTheInvitationIsRefused() {
    invite("friend@example.com");
    assertThat(GREEN_MAIL.waitForIncomingEmail(5000, 1)).isTrue();
    emptyInbox();

    assertThatThrownBy(() -> invite("friend@example.com")).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> invite("ana@example.com")).isInstanceOf(BadRequestException.class);
    logInAs(bob);
    assertThatThrownBy(() -> invite("other@example.com")).isInstanceOf(ForbiddenException.class);

    assertThat(emailArrives()).isFalse();
    assertThat(GREEN_MAIL.getReceivedMessages()).isEmpty();
  }

  @Test
  void noEmailIsSentWhenTheTransactionRollsBack() {
    transactions.executeWithoutResult(
        status -> {
          invite("friend@example.com");
          status.setRollbackOnly();
        });

    assertThat(invitationRepository.count()).isZero();
    assertThat(emailArrives()).isFalse();
  }

  @Test
  void aMailServerThatIsDownDoesNotUndoTheInvitationAndTheFailureIsLogged() {
    GREEN_MAIL.stop();
    try {
      InvitationDTO invitation = invite("friend@example.com");

      assertThat(invitationRepository.findById(invitation.id())).isPresent();
      long deadline = System.currentTimeMillis() + 5000;
      while (logs.list.isEmpty() && System.currentTimeMillis() < deadline) {
        Thread.sleep(50);
      }
      assertThat(logs.list).hasSize(1);
      assertThat(logs.list.getFirst().getLevel()).isEqualTo(Level.WARN);
      assertThat(logs.list.getFirst().getFormattedMessage())
          .contains("invitation " + invitation.id())
          .doesNotContain("friend@example.com");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    } finally {
      GREEN_MAIL.start();
    }
  }
}
