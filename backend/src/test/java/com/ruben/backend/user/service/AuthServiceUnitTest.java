package com.ruben.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.security.JwtService;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.dto.LoginResult;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("B2")
@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

  private static final LoginRequest REQUEST = new LoginRequest("ana@example.com", "secret");

  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtService jwtService;
  @InjectMocks private AuthService authService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private static TripMateUserDetails details() {
    return new TripMateUserDetails(
        UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE));
  }

  @Test
  void loginReturnsTheUserAndAnIssuedToken() {
    TripMateUserDetails details = details();
    when(authenticationManager.authenticate(any()))
        .thenReturn(
            UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));
    when(jwtService.issueToken(details)).thenReturn("signed-token");

    LoginResult result = authService.login(REQUEST);

    assertThat(result.token()).isEqualTo("signed-token");
    assertThat(result.user()).isEqualTo(new UserDTO(7L, "Test USER", "ana@example.com", Role.USER));
  }

  @Test
  void loginAuthenticatesWithTheSubmittedEmailAndPassword() {
    TripMateUserDetails details = details();
    when(authenticationManager.authenticate(any()))
        .thenReturn(
            UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));

    authService.login(REQUEST);

    ArgumentCaptor<Authentication> sent = ArgumentCaptor.forClass(Authentication.class);
    verify(authenticationManager).authenticate(sent.capture());
    assertThat(sent.getValue().getPrincipal()).isEqualTo("ana@example.com");
    assertThat(sent.getValue().getCredentials()).isEqualTo("secret");
    assertThat(sent.getValue().isAuthenticated()).isFalse();
  }

  @Test
  void loginWithBadCredentialsPropagatesAndIssuesNoToken() {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    assertThatThrownBy(() -> authService.login(REQUEST))
        .isInstanceOf(BadCredentialsException.class);
    verifyNoInteractions(jwtService);
  }

  @Test
  void loginOfABlockedUserPropagatesAndIssuesNoToken() {
    when(authenticationManager.authenticate(any())).thenThrow(new LockedException("blocked"));

    assertThatThrownBy(() -> authService.login(REQUEST)).isInstanceOf(LockedException.class);
    verifyNoInteractions(jwtService);
  }

  @Test
  void getCurrentUserReturnsThePrincipalOfTheSecurityContext() {
    TripMateUserDetails details = details();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));

    assertThat(authService.getCurrentUser())
        .isEqualTo(new UserDTO(7L, "Test USER", "ana@example.com", Role.USER));
  }
}
