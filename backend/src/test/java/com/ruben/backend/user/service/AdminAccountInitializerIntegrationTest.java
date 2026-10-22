package com.ruben.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.security.AdminProperties;
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

@Tag("B2")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AdminAccountInitializerIntegrationTest {

  @Autowired private AdminAccountInitializer initializer;
  @Autowired private UserRepository userRepository;
  @Autowired private AdminProperties adminProperties;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    initializer.run(new DefaultApplicationArguments());
  }

  @Test
  void storesTheConfiguredAdministratorWithItsHashedPassword() {
    User admin = userRepository.findByEmail(adminProperties.email()).orElseThrow();

    assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(admin.getName()).isEqualTo(adminProperties.name());
    assertThat(admin.getPassword()).isEqualTo(adminProperties.passwordHash());
  }

  @Test
  void runningAgainDoesNotCreateASecondAdministrator() {
    initializer.run(new DefaultApplicationArguments());

    assertThat(userRepository.findAll()).hasSize(1);
  }

  @Test
  void restoresTheAdministratorAndDemotesOthers() {
    User admin = userRepository.findByEmail(adminProperties.email()).orElseThrow();
    admin.setStatus(UserStatus.BLOCKED);
    userRepository.save(admin);
    userRepository.save(new User("Intruder", "intruder@example.com", "hash", Role.ADMIN));

    initializer.run(new DefaultApplicationArguments());

    assertThat(userRepository.findByEmail(adminProperties.email()).orElseThrow().getStatus())
        .isEqualTo(UserStatus.ACTIVE);
    assertThat(userRepository.findByEmail("intruder@example.com").orElseThrow().getRole())
        .isEqualTo(Role.USER);
  }
}
