package com.ruben.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("B2")
@ExtendWith(MockitoExtension.class)
class SecurityConfigUnitTest {

  private static final String EMAIL = "ana@example.com";

  @Mock private TripMateUserDetailsService userDetailsService;

  private final SecurityConfig config = new SecurityConfig();
  private final PasswordEncoder passwordEncoder = config.passwordEncoder();
  private AuthenticationManager authenticationManager;

  @BeforeEach
  void setUp() {
    authenticationManager = config.authenticationManager(userDetailsService, passwordEncoder);
  }

  private void stubUser(UserStatus status) {
    User user = UserTestData.user(7L, EMAIL, Role.USER, status);
    user.setPassword(passwordEncoder.encode("right-password"));
    when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new TripMateUserDetails(user));
  }

  private Authentication attempt(String password) {
    return authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, password));
  }

  @Test
  void passwordsAreHashedWithBcrypt() {
    String hash = passwordEncoder.encode("secret");

    assertThat(hash).startsWith("$2").isNotEqualTo("secret");
    assertThat(passwordEncoder.matches("secret", hash)).isTrue();
    assertThat(passwordEncoder.matches("other", hash)).isFalse();
  }

  @Test
  void anActiveUserWithTheRightPasswordIsAuthenticated() {
    stubUser(UserStatus.ACTIVE);

    Authentication authentication = attempt("right-password");

    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getPrincipal()).isInstanceOf(TripMateUserDetails.class);
  }

  @Test
  void aWrongPasswordIsRejected() {
    stubUser(UserStatus.ACTIVE);

    assertThatThrownBy(() -> attempt("wrong")).isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void anUnknownUserIsRejectedAsBadCredentials() {
    when(userDetailsService.loadUserByUsername(EMAIL))
        .thenThrow(new UsernameNotFoundException("not found"));

    assertThatThrownBy(() -> attempt("whatever")).isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void aBlockedUserWithTheRightPasswordIsLocked() {
    stubUser(UserStatus.BLOCKED);

    assertThatThrownBy(() -> attempt("right-password")).isInstanceOf(LockedException.class);
  }

  @Test
  void aBlockedUserWithTheWrongPasswordIsNotToldTheAccountIsBlocked() {
    stubUser(UserStatus.BLOCKED);

    assertThatThrownBy(() -> attempt("wrong")).isInstanceOf(BadCredentialsException.class);
  }
}
