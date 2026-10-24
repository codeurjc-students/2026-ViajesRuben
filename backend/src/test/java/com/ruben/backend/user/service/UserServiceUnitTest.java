package com.ruben.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("B2")
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

  private static final String ADMIN_EMAIL = "admin@tripmate.com";

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  void setUp() {
    AdminProperties admin = new AdminProperties(ADMIN_EMAIL, "Admin", "unused");
    userService = new UserService(userRepository, passwordEncoder, admin);
  }

  @Test
  void registerSavesAnActiveRegularUserWithTheEncodedPassword() {
    when(passwordEncoder.encode("secret-pass")).thenReturn("encoded");
    when(userRepository.saveAndFlush(any(User.class)))
        .thenReturn(UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE));

    UserDTO result =
        userService.register(new RegisterRequest("  Ana  ", " Ana@Example.COM ", "secret-pass"));

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getName()).isEqualTo("Ana");
    assertThat(saved.getValue().getEmail()).isEqualTo("ana@example.com");
    assertThat(saved.getValue().getPassword()).isEqualTo("encoded");
    assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
    assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(result).isEqualTo(new UserDTO(7L, "Test USER", "ana@example.com", Role.USER));
  }

  @Test
  void registerRejectsAnEmailThatIsAlreadyRegistered() {
    when(userRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);
    RegisterRequest request = new RegisterRequest("Ana", "ana@example.com", "secret-pass");

    assertThatThrownBy(() -> userService.register(request))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Email is already registered");
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void registerRejectsTheAdministratorEmailInAnyCase() {
    RegisterRequest request = new RegisterRequest("Mallory", "ADMIN@TripMate.com", "secret-pass");

    assertThatThrownBy(() -> userService.register(request))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Email is already registered");
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void registerTurnsALostRaceOnTheUniqueEmailIntoAConflict() {
    when(passwordEncoder.encode("secret-pass")).thenReturn("encoded");
    when(userRepository.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));
    RegisterRequest request = new RegisterRequest("Ana", "ana@example.com", "secret-pass");

    assertThatThrownBy(() -> userService.register(request))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Email is already registered");
  }
}
