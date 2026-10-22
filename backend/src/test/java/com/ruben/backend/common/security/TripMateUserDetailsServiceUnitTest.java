package com.ruben.backend.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Tag("B2")
@ExtendWith(MockitoExtension.class)
class TripMateUserDetailsServiceUnitTest {

  @Mock private UserRepository userRepository;
  @InjectMocks private TripMateUserDetailsService service;

  @Test
  void loadsAnActiveUserByEmail() {
    when(userRepository.findByEmail("ana@example.com"))
        .thenReturn(
            Optional.of(UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE)));

    TripMateUserDetails details = service.loadUserByUsername("ana@example.com");

    assertThat(details.getId()).isEqualTo(7L);
    assertThat(details.getUsername()).isEqualTo("ana@example.com");
    assertThat(details.getPassword()).isEqualTo("hash");
    assertThat(details.getRole()).isEqualTo(Role.USER);
    assertThat(details.isAccountNonLocked()).isTrue();
    assertThat(details.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_USER");
  }

  @Test
  void aBlockedUserIsLoadedButMarkedAsLocked() {
    when(userRepository.findByEmail("ana@example.com"))
        .thenReturn(
            Optional.of(UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.BLOCKED)));

    assertThat(service.loadUserByUsername("ana@example.com").isAccountNonLocked()).isFalse();
  }

  @Test
  void anUnknownEmailIsNotFound() {
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadUserByUsername("ghost@example.com"))
        .isInstanceOf(UsernameNotFoundException.class);
  }

  @Test
  void loadsAUserById() {
    when(userRepository.findById(7L))
        .thenReturn(
            Optional.of(UserTestData.user(7L, "ana@example.com", Role.ADMIN, UserStatus.ACTIVE)));

    assertThat(service.loadUserById(7L).getRole()).isEqualTo(Role.ADMIN);
  }

  @Test
  void anUnknownIdIsNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadUserById(99L))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
