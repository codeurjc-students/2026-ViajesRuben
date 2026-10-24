package com.ruben.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("B2")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserServiceIntegrationTest {

  private static final String PASSWORD = "secret-pass";

  @Autowired private UserService userService;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    adminAccountInitializer.run(new DefaultApplicationArguments());
  }

  @Test
  void registerStoresAnActiveRegularUserWithABcryptHash() {
    UserDTO created = userService.register(new RegisterRequest("Ana", "Ana@Example.com", PASSWORD));

    User stored = userRepository.findById(created.id()).orElseThrow();
    assertThat(stored.getEmail()).isEqualTo("ana@example.com");
    assertThat(stored.getRole()).isEqualTo(Role.USER);
    assertThat(stored.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(stored.getRegistrationDate()).isEqualTo(LocalDate.now());
    assertThat(stored.getPassword()).isNotEqualTo(PASSWORD);
    assertThat(passwordEncoder.matches(PASSWORD, stored.getPassword())).isTrue();
  }

  @Test
  void aRegisteredUserCanLogInWithTheChosenPassword() {
    userService.register(new RegisterRequest("Ana", "ana@example.com", PASSWORD));

    assertThat(authService.login(new LoginRequest("ana@example.com", PASSWORD)).user().name())
        .isEqualTo("Ana");
  }

  @Test
  void registeringTheSameEmailInAnotherCaseIsAConflict() {
    userService.register(new RegisterRequest("Ana", "ana@example.com", PASSWORD));
    RegisterRequest duplicate = new RegisterRequest("Other", "ANA@EXAMPLE.COM", PASSWORD);

    assertThatThrownBy(() -> userService.register(duplicate)).isInstanceOf(ConflictException.class);
    assertThat(userRepository.count()).isEqualTo(2);
  }

  @Test
  void theAdministratorEmailCannotBeRegistered() {
    RegisterRequest request = new RegisterRequest("Mallory", "ADMIN@test.tripmate.com", PASSWORD);

    assertThatThrownBy(() -> userService.register(request)).isInstanceOf(ConflictException.class);
    assertThat(userRepository.count()).isEqualTo(1);
  }
}
