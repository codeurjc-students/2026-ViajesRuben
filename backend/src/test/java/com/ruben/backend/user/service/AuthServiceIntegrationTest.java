package com.ruben.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.dto.LoginResult;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Tag("B2")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthServiceIntegrationTest {

  /** Matches the BCrypt hash configured for the test administrator. */
  private static final String ADMIN_PASSWORD = "Admin-test-1";

  private static final String USER_PASSWORD = "User-pass-1";

  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;
  @Autowired private AdminProperties adminProperties;
  @Autowired private JwtDecoder jwtDecoder;

  private User ana;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    adminAccountInitializer.run(new DefaultApplicationArguments());
    ana =
        userRepository.save(
            new User("Ana", "ana@example.com", passwordEncoder.encode(USER_PASSWORD), Role.USER));
    User blocked =
        new User(
            "Blocked", "blocked@example.com", passwordEncoder.encode(USER_PASSWORD), Role.USER);
    blocked.setStatus(UserStatus.BLOCKED);
    userRepository.save(blocked);
  }

  @Test
  void theConfiguredAdministratorLogsInWithItsPlainPassword() {
    LoginResult result =
        authService.login(new LoginRequest(adminProperties.email(), ADMIN_PASSWORD));

    assertThat(result.user().role()).isEqualTo(Role.ADMIN);
    assertThat(result.user().email()).isEqualTo(adminProperties.email());
    assertThat(result.token()).isNotBlank();
  }

  @Test
  void aRegularUserLogsInAndGetsATokenForItsId() {
    LoginResult result = authService.login(new LoginRequest("ana@example.com", USER_PASSWORD));

    assertThat(result.user().role()).isEqualTo(Role.USER);
    assertThat(result.user().id()).isEqualTo(ana.getId());
    assertThat(jwtDecoder.decode(result.token()).getSubject())
        .isEqualTo(String.valueOf(ana.getId()));
  }

  @Test
  void aWrongPasswordIsRejected() {
    LoginRequest request = new LoginRequest("ana@example.com", "wrong");

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void anUnknownEmailIsRejectedLikeAWrongPassword() {
    LoginRequest request = new LoginRequest("ghost@example.com", USER_PASSWORD);

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void aBlockedUserCannotLogInEvenWithTheRightPassword() {
    LoginRequest request = new LoginRequest("blocked@example.com", USER_PASSWORD);

    assertThatThrownBy(() -> authService.login(request)).isInstanceOf(LockedException.class);
  }

  @Test
  void aBlockedUserWithTheWrongPasswordGetsNoHintThatTheAccountExists() {
    LoginRequest request = new LoginRequest("blocked@example.com", "wrong");

    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void aUserCanLogInAgainAfterBeingUnblocked() {
    User blocked = userRepository.findByEmail("blocked@example.com").orElseThrow();
    blocked.setStatus(UserStatus.ACTIVE);
    userRepository.save(blocked);

    assertThat(authService.login(new LoginRequest("blocked@example.com", USER_PASSWORD)).token())
        .isNotBlank();
  }
}
