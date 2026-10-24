package com.ruben.backend.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.security.JwtService;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.dto.LoginResult;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.service.AuthService;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

@Tag("B2")
@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

  private static final UserDTO ANA =
      new UserDTO(7L, "Ana", "ana@example.com", Role.USER, null, LocalDate.of(2026, 10, 1));

  @Mock private AuthService authService;
  @Mock private JwtService jwtService;
  @InjectMocks private AuthController controller;

  @Test
  void loginReturnsTheUserAndSetsTheAuthCookie() {
    LoginRequest request = new LoginRequest("ana@example.com", "secret");
    ResponseCookie cookie = ResponseCookie.from("AuthToken", "signed-token").httpOnly(true).build();
    when(authService.login(request)).thenReturn(new LoginResult(ANA, "signed-token"));
    when(jwtService.authCookie("signed-token")).thenReturn(cookie);

    ResponseEntity<UserDTO> response = controller.login(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(ANA);
    assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isEqualTo(cookie.toString());
  }

  @Test
  void logoutReturns204AndClearsTheCookie() {
    ResponseCookie cleared = ResponseCookie.from("AuthToken", "").maxAge(Duration.ZERO).build();
    when(jwtService.clearedCookie()).thenReturn(cleared);

    ResponseEntity<Void> response = controller.logout();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
        .isEqualTo(cleared.toString());
  }

  @Test
  void meReturnsTheCurrentUser() {
    when(authService.getCurrentUser()).thenReturn(ANA);

    assertThat(controller.me()).isEqualTo(ANA);
  }
}
