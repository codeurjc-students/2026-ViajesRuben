package com.ruben.backend.user.service;

import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.service.ImageService;
import com.ruben.backend.user.dto.ChangePasswordRequest;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UpdateProfileRequest;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);
  private static final String EMAIL_TAKEN = "Email is already registered";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AdminProperties adminProperties;
  private final AuthService authService;
  private final ImageService imageService;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AdminProperties adminProperties,
      AuthService authService,
      ImageService imageService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.adminProperties = adminProperties;
    this.authService = authService;
    this.imageService = imageService;
  }

  /**
   * Creates a regular user. The administrator email is refused like a taken one: the administrator
   * account is rewritten from the configuration on every start-up.
   *
   * @throws ConflictException the email belongs to another account or to the administrator
   */
  public UserDTO register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    ensureEmailAvailable(email);
    User user =
        new User(
            request.name().trim(), email, passwordEncoder.encode(request.password()), Role.USER);
    user = saveCheckingEmail(user);
    log.info("Registered user {}", user.getId());
    return UserDTO.from(user);
  }

  /**
   * The full profile, only for the user themselves.
   *
   * @throws ResourceNotFoundException no user has that id
   * @throws ForbiddenException it is another user's profile
   */
  public UserDTO getProfile(Long id) {
    return UserDTO.from(findOwnUser(id));
  }

  /**
   * Changes the name and email, with the registration rules for the email. The session keeps
   * working: the token identifies the user by id, not by email.
   *
   * @throws ForbiddenException another user's profile, or the administrator, whose details come
   *     from the configuration
   * @throws ConflictException the new email belongs to another account or to the administrator
   */
  @Transactional
  public UserDTO updateProfile(Long id, UpdateProfileRequest request) {
    User user = findOwnUser(id);
    ensureNotAdministrator(user);
    String email = normalizeEmail(request.email());
    if (!email.equalsIgnoreCase(user.getEmail())) {
      ensureEmailAvailable(email);
    }
    user.setName(request.name().trim());
    user.setEmail(email);
    user = saveCheckingEmail(user);
    log.info("User {} updated their profile", user.getId());
    return UserDTO.from(user);
  }

  /**
   * @throws BadRequestException the current password is wrong
   * @throws ForbiddenException another user's profile, or the administrator
   */
  @Transactional
  public void changePassword(Long id, ChangePasswordRequest request) {
    User user = findOwnUser(id);
    ensureNotAdministrator(user);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new BadRequestException("Current password is incorrect");
    }
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    log.info("User {} changed their password", user.getId());
  }

  /**
   * Stores the picture (same checks as any image) as the user's avatar and deletes the previous
   * one, so no unused image is left behind.
   */
  @Transactional
  public ImageResult replaceAvatar(Long id, MultipartFile file) {
    User user = findOwnUser(id);
    Image previous = user.getAvatar();
    Image avatar = imageService.save(file);
    user.setAvatar(avatar);
    userRepository.save(user);
    if (previous != null) {
      imageService.delete(previous.getId());
    }
    log.info("User {} {} their avatar", user.getId(), previous == null ? "added" : "replaced");
    return new ImageResult(ImageDTO.from(avatar), previous == null);
  }

  /** Removes the avatar; doing it when there is none changes nothing. */
  @Transactional
  public void deleteAvatar(Long id) {
    User user = findOwnUser(id);
    Image avatar = user.getAvatar();
    if (avatar == null) {
      return;
    }
    user.setAvatar(null);
    userRepository.save(user);
    imageService.delete(avatar.getId());
    log.info("User {} deleted their avatar", user.getId());
  }

  /** Any logged-in user may see an avatar: trip mates are shown with theirs. */
  @Transactional(readOnly = true)
  public ImageContentDTO findAvatarContent(Long id) {
    Image avatar = findUser(id).getAvatar();
    if (avatar == null) {
      throw new ResourceNotFoundException("The user has no avatar");
    }
    return imageService.findContent(avatar.getId());
  }

  private User findUser(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  /** 404 comes before 403, so a missing user is reported as missing to anyone. */
  private User findOwnUser(Long id) {
    User user = findUser(id);
    if (!user.getId().equals(authService.getCurrentUser().id())) {
      throw new ForbiddenException("Only the user can access their own profile");
    }
    return user;
  }

  /**
   * The administrator's name, email and password are written from the configuration on every
   * start-up: a change would be lost, and a new email would create a second administrator.
   */
  private static void ensureNotAdministrator(User user) {
    if (user.getRole() == Role.ADMIN) {
      throw new ForbiddenException("The administrator account is managed by the configuration");
    }
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private void ensureEmailAvailable(String email) {
    if (email.equalsIgnoreCase(adminProperties.email())
        || userRepository.existsByEmailIgnoreCase(email)) {
      throw new ConflictException(EMAIL_TAKEN);
    }
  }

  private User saveCheckingEmail(User user) {
    try {
      return userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException e) {
      // Another request took the same email between the check and the write.
      throw new ConflictException(EMAIL_TAKEN);
    }
  }
}
