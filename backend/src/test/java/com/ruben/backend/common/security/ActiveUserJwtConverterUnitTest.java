package com.ruben.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag("B2")
@ExtendWith(MockitoExtension.class)
class ActiveUserJwtConverterUnitTest {

  @Mock private TripMateUserDetailsService userDetailsService;
  @InjectMocks private ActiveUserJwtConverter converter;

  private static Jwt tokenFor(String subject) {
    return Jwt.withTokenValue("token").header("alg", "HS256").subject(subject).build();
  }

  @Test
  void anActiveUserGetsTheAuthorityOfTheStoredRole() {
    TripMateUserDetails user =
        new TripMateUserDetails(
            UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE));
    when(userDetailsService.loadUserById(7L)).thenReturn(user);

    UsernamePasswordAuthenticationToken authentication = converter.convert(tokenFor("7"));

    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getPrincipal()).isSameAs(user);
    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_USER");
  }

  @Test
  void theAdministratorGetsTheAdminAuthority() {
    TripMateUserDetails admin =
        new TripMateUserDetails(
            UserTestData.user(1L, "admin@example.com", Role.ADMIN, UserStatus.ACTIVE));
    when(userDetailsService.loadUserById(1L)).thenReturn(admin);

    assertThat(converter.convert(tokenFor("1")).getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_ADMIN");
  }

  @Test
  void aBlockedUserIsRejectedEvenWithAValidToken() {
    TripMateUserDetails blocked =
        new TripMateUserDetails(
            UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.BLOCKED));
    when(userDetailsService.loadUserById(7L)).thenReturn(blocked);
    Jwt token = tokenFor("7");

    assertThatThrownBy(() -> converter.convert(token)).isInstanceOf(LockedException.class);
  }

  @Test
  void aTokenOfAUserThatNoLongerExistsIsRejected() {
    UsernameNotFoundException gone = new UsernameNotFoundException("gone");
    when(userDetailsService.loadUserById(99L)).thenThrow(gone);
    Jwt token = tokenFor("99");

    assertThatThrownBy(() -> converter.convert(token)).isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void aNonNumericSubjectIsRejectedWithoutTouchingTheDatabase() {
    Jwt token = tokenFor("not-a-number");

    assertThatThrownBy(() -> converter.convert(token)).isInstanceOf(BadCredentialsException.class);
    verifyNoInteractions(userDetailsService);
  }
}
