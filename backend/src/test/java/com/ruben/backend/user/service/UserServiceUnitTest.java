package com.ruben.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.error.ForbiddenException;
import com.ruben.backend.common.error.ResourceNotFoundException;
import com.ruben.backend.common.security.AdminProperties;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageContentDTO;
import com.ruben.backend.image.dto.ImageResult;
import com.ruben.backend.image.entity.Image;
import com.ruben.backend.image.service.ImageService;
import com.ruben.backend.user.UserTestData;
import com.ruben.backend.user.dto.ChangePasswordRequest;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UpdateProfileRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("B2")
@Tag("B3")
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

  private static final String ADMIN_EMAIL = "admin@tripmate.com";
  private static final MockMultipartFile PICTURE =
      new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthService authService;
  @Mock private ImageService imageService;

  private UserService userService;

  @BeforeEach
  void setUp() {
    AdminProperties admin = new AdminProperties(ADMIN_EMAIL, "Admin", "unused");
    userService =
        new UserService(userRepository, passwordEncoder, admin, authService, imageService);
  }

  /** Ana (id 7) exists and is the logged-in user. */
  private User loggedInAna() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    when(userRepository.findById(7L)).thenReturn(Optional.of(ana));
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(ana));
    return ana;
  }

  private User loggedInAdmin() {
    User admin = UserTestData.user(1L, ADMIN_EMAIL, Role.ADMIN, UserStatus.ACTIVE);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(admin));
    return admin;
  }

  /** Ana is logged in and asks for Bob (id 8). */
  private void anaAsksForBob() {
    User ana = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    User bob = UserTestData.user(8L, "bob@example.com", Role.USER, UserStatus.ACTIVE);
    when(userRepository.findById(8L)).thenReturn(Optional.of(bob));
    when(authService.getCurrentUser()).thenReturn(UserDTO.from(ana));
  }

  private static Image image() {
    Image image = new Image(ImageTestData.png(10), "image/png");
    ReflectionTestUtils.setField(image, "id", UUID.randomUUID());
    return image;
  }

  // --- B2: registration ---

  @Test
  void registerSavesAnActiveRegularUserWithTheEncodedPassword() {
    when(passwordEncoder.encode("secret-pass")).thenReturn("encoded");
    User stored = UserTestData.user(7L, "ana@example.com", Role.USER, UserStatus.ACTIVE);
    when(userRepository.saveAndFlush(any(User.class))).thenReturn(stored);

    UserDTO result =
        userService.register(new RegisterRequest("  Ana  ", " Ana@Example.COM ", "secret-pass"));

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getName()).isEqualTo("Ana");
    assertThat(saved.getValue().getEmail()).isEqualTo("ana@example.com");
    assertThat(saved.getValue().getPassword()).isEqualTo("encoded");
    assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
    assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(result)
        .isEqualTo(
            new UserDTO(
                7L, "Test USER", "ana@example.com", Role.USER, null, stored.getRegistrationDate()));
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

  // --- B3: read the profile ---

  @Test
  void getProfileReturnsTheOwnProfileWithTheAvatarId() {
    User ana = loggedInAna();
    Image avatar = image();
    ana.setAvatar(avatar);

    UserDTO profile = userService.getProfile(7L);

    assertThat(profile.id()).isEqualTo(7L);
    assertThat(profile.email()).isEqualTo("ana@example.com");
    assertThat(profile.avatarId()).isEqualTo(avatar.getId());
    assertThat(profile.registrationDate()).isEqualTo(ana.getRegistrationDate());
  }

  @Test
  void getProfileOfAnotherUserIsForbidden() {
    anaAsksForBob();

    assertThatThrownBy(() -> userService.getProfile(8L))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Only the user can access their own profile");
  }

  @Test
  void getProfileOfAMissingUserIsNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getProfile(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("User not found");
    verifyNoInteractions(authService);
  }

  // --- B3: edit name and email ---

  @Test
  void updateProfileSavesTheTrimmedNameAndTheNormalizedEmail() {
    loggedInAna();
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

    UserDTO result =
        userService.updateProfile(7L, new UpdateProfileRequest("  Ana Ruiz ", " Ana.R@Mail.COM "));

    assertThat(result.name()).isEqualTo("Ana Ruiz");
    assertThat(result.email()).isEqualTo("ana.r@mail.com");
    verify(userRepository).existsByEmailIgnoreCase("ana.r@mail.com");
  }

  @Test
  void updateProfileKeepingTheSameEmailDoesNotCheckIt() {
    loggedInAna();
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

    UserDTO result =
        userService.updateProfile(7L, new UpdateProfileRequest("Ana", "ANA@example.com"));

    assertThat(result.email()).isEqualTo("ana@example.com");
    verify(userRepository, never()).existsByEmailIgnoreCase(any());
  }

  @Test
  void updateProfileRejectsAnEmailOfAnotherAccount() {
    loggedInAna();
    when(userRepository.existsByEmailIgnoreCase("bob@example.com")).thenReturn(true);
    UpdateProfileRequest request = new UpdateProfileRequest("Ana", "bob@example.com");

    assertThatThrownBy(() -> userService.updateProfile(7L, request))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Email is already registered");
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void updateProfileRejectsTheAdministratorEmail() {
    loggedInAna();
    UpdateProfileRequest request = new UpdateProfileRequest("Ana", "Admin@TripMate.com");

    assertThatThrownBy(() -> userService.updateProfile(7L, request))
        .isInstanceOf(ConflictException.class)
        .hasMessage("Email is already registered");
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void updateProfileTurnsALostRaceOnTheUniqueEmailIntoAConflict() {
    loggedInAna();
    when(userRepository.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));
    UpdateProfileRequest request = new UpdateProfileRequest("Ana", "new@example.com");

    assertThatThrownBy(() -> userService.updateProfile(7L, request))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void updateProfileOfAnotherUserIsForbidden() {
    anaAsksForBob();
    UpdateProfileRequest request = new UpdateProfileRequest("Bob", "bob@example.com");

    assertThatThrownBy(() -> userService.updateProfile(8L, request))
        .isInstanceOf(ForbiddenException.class);
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void theAdministratorCannotEditTheirProfile() {
    loggedInAdmin();
    UpdateProfileRequest request = new UpdateProfileRequest("Boss", "boss@example.com");

    assertThatThrownBy(() -> userService.updateProfile(1L, request))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("The administrator account is managed by the configuration");
    verify(userRepository, never()).saveAndFlush(any());
  }

  // --- B3: change the password ---

  @Test
  void changePasswordStoresTheNewEncodedPassword() {
    User ana = loggedInAna();
    when(passwordEncoder.matches("old-secret", "hash")).thenReturn(true);
    when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");

    userService.changePassword(7L, new ChangePasswordRequest("old-secret", "new-secret"));

    assertThat(ana.getPassword()).isEqualTo("new-hash");
    verify(userRepository).save(ana);
  }

  @Test
  void changePasswordWithAWrongCurrentPasswordIsABadRequest() {
    loggedInAna();
    when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
    ChangePasswordRequest request = new ChangePasswordRequest("wrong", "new-secret");

    assertThatThrownBy(() -> userService.changePassword(7L, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Current password is incorrect");
    verify(userRepository, never()).save(any());
  }

  @Test
  void changePasswordOfAnotherUserIsForbidden() {
    anaAsksForBob();
    ChangePasswordRequest request = new ChangePasswordRequest("old-secret", "new-secret");

    assertThatThrownBy(() -> userService.changePassword(8L, request))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  void theAdministratorCannotChangeTheirPassword() {
    loggedInAdmin();
    ChangePasswordRequest request = new ChangePasswordRequest("admin", "new-secret");

    assertThatThrownBy(() -> userService.changePassword(1L, request))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(passwordEncoder);
  }

  // --- B3: avatar ---

  @Test
  void theFirstAvatarIsStoredAndReportedAsCreated() {
    User ana = loggedInAna();
    Image stored = image();
    when(imageService.save(PICTURE)).thenReturn(stored);

    ImageResult result = userService.replaceAvatar(7L, PICTURE);

    assertThat(result.created()).isTrue();
    assertThat(result.image().id()).isEqualTo(stored.getId());
    assertThat(ana.getAvatar()).isSameAs(stored);
    verify(userRepository).save(ana);
    verify(imageService, never()).delete(any());
  }

  @Test
  void replacingTheAvatarDeletesThePreviousImage() {
    User ana = loggedInAna();
    Image previous = image();
    ana.setAvatar(previous);
    Image stored = image();
    when(imageService.save(PICTURE)).thenReturn(stored);

    ImageResult result = userService.replaceAvatar(7L, PICTURE);

    assertThat(result.created()).isFalse();
    assertThat(ana.getAvatar()).isSameAs(stored);
    verify(imageService).delete(previous.getId());
  }

  @Test
  void theAdministratorCanChangeTheirAvatar() {
    loggedInAdmin();
    when(imageService.save(PICTURE)).thenReturn(image());

    assertThat(userService.replaceAvatar(1L, PICTURE).created()).isTrue();
  }

  @Test
  void replacingTheAvatarOfAnotherUserIsForbiddenAndStoresNothing() {
    anaAsksForBob();

    assertThatThrownBy(() -> userService.replaceAvatar(8L, PICTURE))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(imageService);
  }

  @Test
  void anInvalidAvatarIsRejectedAndTheUserIsNotChanged() {
    User ana = loggedInAna();
    when(imageService.save(PICTURE)).thenThrow(new BadRequestException("bad"));

    assertThatThrownBy(() -> userService.replaceAvatar(7L, PICTURE))
        .isInstanceOf(BadRequestException.class);
    assertThat(ana.getAvatar()).isNull();
    verify(userRepository, never()).save(any());
  }

  @Test
  void deleteAvatarUnlinksAndDeletesTheImage() {
    User ana = loggedInAna();
    Image avatar = image();
    ana.setAvatar(avatar);

    userService.deleteAvatar(7L);

    assertThat(ana.getAvatar()).isNull();
    verify(userRepository).save(ana);
    verify(imageService).delete(avatar.getId());
  }

  @Test
  void deleteAvatarWithoutAvatarChangesNothing() {
    loggedInAna();

    userService.deleteAvatar(7L);

    verify(userRepository, never()).save(any());
    verifyNoInteractions(imageService);
  }

  @Test
  void deleteAvatarOfAnotherUserIsForbidden() {
    anaAsksForBob();

    assertThatThrownBy(() -> userService.deleteAvatar(8L)).isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(imageService);
  }

  @Test
  void anyLoggedInUserCanReadAnAvatar() {
    User bob = UserTestData.user(8L, "bob@example.com", Role.USER, UserStatus.ACTIVE);
    Image avatar = image();
    bob.setAvatar(avatar);
    when(userRepository.findById(8L)).thenReturn(Optional.of(bob));
    ImageContentDTO content = new ImageContentDTO(avatar.getData(), "image/png");
    when(imageService.findContent(avatar.getId())).thenReturn(content);

    assertThat(userService.findAvatarContent(8L)).isEqualTo(content);
    verifyNoInteractions(authService);
  }

  @Test
  void readingTheAvatarOfAUserWithoutOneIsNotFound() {
    User bob = UserTestData.user(8L, "bob@example.com", Role.USER, UserStatus.ACTIVE);
    when(userRepository.findById(8L)).thenReturn(Optional.of(bob));

    assertThatThrownBy(() -> userService.findAvatarContent(8L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("The user has no avatar");
  }

  @Test
  void readingTheAvatarOfAMissingUserIsNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.findAvatarContent(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("User not found");
  }
}
