package com.ruben.backend.user.service;

import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the database in line with the administrator defined in configuration on every start-up, so
 * there is always exactly one administrator and it cannot be created, edited or promoted through
 * the API.
 */
@Component
@Order(0)
public class AdminAccountInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

  private final UserRepository userRepository;
  private final AdminProperties adminProperties;

  public AdminAccountInitializer(UserRepository userRepository, AdminProperties adminProperties) {
    this.userRepository = userRepository;
    this.adminProperties = adminProperties;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    String email = adminProperties.email();
    User admin =
        userRepository
            .findByEmail(email)
            .orElseGet(
                () ->
                    new User(
                        adminProperties.name(), email, adminProperties.passwordHash(), Role.ADMIN));
    admin.setName(adminProperties.name());
    admin.setPassword(adminProperties.passwordHash());
    admin.setRole(Role.ADMIN);
    admin.setStatus(UserStatus.ACTIVE);
    userRepository.save(admin);

    int demoted = userRepository.demoteOtherAdmins(email);
    if (demoted > 0) {
      log.warn(
          "Demoted {} user(s) that were administrators but are not the configured one", demoted);
    }
    log.info("Administrator account ready for {}", email);
  }
}
