package com.ruben.backend.user.service;

import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);
  private static final String EMAIL_TAKEN = "Email is already registered";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AdminProperties adminProperties;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AdminProperties adminProperties) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.adminProperties = adminProperties;
  }

  /**
   * Creates a regular user. The administrator email is refused like a taken one: the administrator
   * account is rewritten from the configuration on every start-up.
   *
   * @throws ConflictException the email belongs to another account or to the administrator
   */
  public UserDTO register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    if (email.equalsIgnoreCase(adminProperties.email())
        || userRepository.existsByEmailIgnoreCase(email)) {
      throw new ConflictException(EMAIL_TAKEN);
    }
    User user =
        new User(
            request.name().trim(), email, passwordEncoder.encode(request.password()), Role.USER);
    try {
      user = userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException e) {
      // Another request registered the same email between the check and the insert.
      throw new ConflictException(EMAIL_TAKEN);
    }
    log.info("Registered user {}", user.getId());
    return UserDTO.from(user);
  }
}
