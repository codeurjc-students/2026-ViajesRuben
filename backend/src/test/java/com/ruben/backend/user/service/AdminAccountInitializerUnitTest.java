package com.ruben.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@Tag("B2")
@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerUnitTest {

  private static final String EMAIL = "admin@tripmate.com";
  private static final String HASH = "$2a$10$opop/gseD0EqqQJ4WJcxSeasiIejoCJni6fpKYxNFfDSgyVxq6VMS";

  @Mock private UserRepository userRepository;

  private AdminAccountInitializer initializer;

  @BeforeEach
  void setUp() {
    initializer =
        new AdminAccountInitializer(
            userRepository, new AdminProperties(EMAIL, "Administrator", HASH));
  }

  @Test
  void createsTheAdministratorWhenItDoesNotExist() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    initializer.run(new DefaultApplicationArguments());

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    assertThat(saved.getValue().getEmail()).isEqualTo(EMAIL);
    assertThat(saved.getValue().getName()).isEqualTo("Administrator");
    assertThat(saved.getValue().getPassword()).isEqualTo(HASH);
    assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
    assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void overwritesAnExistingAccountWithTheConfiguredAdministrator() {
    User existing = new User("Someone", EMAIL, "old-hash", Role.USER);
    existing.setStatus(UserStatus.BLOCKED);
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

    initializer.run(new DefaultApplicationArguments());

    verify(userRepository).save(existing);
    assertThat(existing.getName()).isEqualTo("Administrator");
    assertThat(existing.getPassword()).isEqualTo(HASH);
    assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
    assertThat(existing.getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void demotesAnyOtherAdministrator() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.demoteOtherAdmins(EMAIL)).thenReturn(1);

    initializer.run(new DefaultApplicationArguments());

    verify(userRepository).demoteOtherAdmins(EMAIL);
    verify(userRepository).save(any(User.class));
  }
}
