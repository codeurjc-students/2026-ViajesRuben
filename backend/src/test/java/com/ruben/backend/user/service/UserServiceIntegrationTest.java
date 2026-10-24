package com.ruben.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.common.error.BadRequestException;
import com.ruben.backend.common.error.ConflictException;
import com.ruben.backend.common.security.ActiveUserJwtConverter;
import com.ruben.backend.common.security.JwtService;
import com.ruben.backend.common.security.TripMateUserDetails;
import com.ruben.backend.common.security.TripMateUserDetailsService;
import com.ruben.backend.image.ImageTestData;
import com.ruben.backend.image.dto.ImageDTO;
import com.ruben.backend.image.repository.ImageRepository;
import com.ruben.backend.user.dto.ChangePasswordRequest;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UpdateProfileRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import com.ruben.backend.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Tag("B2")
@Tag("B3")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserServiceIntegrationTest {

  private static final String PASSWORD = "secret-pass";

  @Autowired private UserService userService;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AdminAccountInitializer adminAccountInitializer;
  @Autowired private ImageRepository imageRepository;
  @Autowired private TripMateUserDetailsService userDetailsService;
  @Autowired private JwtService jwtService;
  @Autowired private JwtDecoder jwtDecoder;
  @Autowired private ActiveUserJwtConverter jwtConverter;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    imageRepository.deleteAll();
    adminAccountInitializer.run(new DefaultApplicationArguments());
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  /** Registers Ana and makes her the user of the current request, as the JWT filter would. */
  private UserDTO registerAndLogInAna() {
    UserDTO ana = userService.register(new RegisterRequest("Ana", "ana@example.com", PASSWORD));
    TripMateUserDetails details = userDetailsService.loadUserById(ana.id());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities()));
    return ana;
  }

  private static MockMultipartFile png() {
    return new MockMultipartFile("file", "a.png", "image/png", ImageTestData.png(100));
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

  @Test
  void aTokenIssuedBeforeAnEmailChangeStillIdentifiesTheUser() {
    UserDTO ana = registerAndLogInAna();
    String token = jwtService.issueToken(userDetailsService.loadUserById(ana.id()));

    userService.updateProfile(ana.id(), new UpdateProfileRequest("Ana R", "ana.r@example.com"));

    TripMateUserDetails resolved =
        (TripMateUserDetails) jwtConverter.convert(jwtDecoder.decode(token)).getPrincipal();
    assertThat(resolved.getId()).isEqualTo(ana.id());
    assertThat(resolved.getUsername()).isEqualTo("ana.r@example.com");
    assertThat(resolved.getName()).isEqualTo("Ana R");
  }

  @Test
  void afterAnEmailChangeOnlyTheNewEmailLogsIn() {
    UserDTO ana = registerAndLogInAna();

    userService.updateProfile(ana.id(), new UpdateProfileRequest("Ana", "ana.r@example.com"));

    assertThat(authService.login(new LoginRequest("ana.r@example.com", PASSWORD)).user().id())
        .isEqualTo(ana.id());
    LoginRequest oldEmail = new LoginRequest("ana@example.com", PASSWORD);
    assertThatThrownBy(() -> authService.login(oldEmail))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void anEmailOfAnotherAccountIsAConflictAndNothingChanges() {
    userService.register(new RegisterRequest("Bob", "bob@example.com", PASSWORD));
    UserDTO ana = registerAndLogInAna();
    UpdateProfileRequest request = new UpdateProfileRequest("Ana", "BOB@example.com");

    assertThatThrownBy(() -> userService.updateProfile(ana.id(), request))
        .isInstanceOf(ConflictException.class);
    assertThat(userRepository.findById(ana.id()).orElseThrow().getEmail())
        .isEqualTo("ana@example.com");
  }

  @Test
  void aChangedPasswordIsStoredAsBcryptAndLogsIn() {
    UserDTO ana = registerAndLogInAna();

    userService.changePassword(ana.id(), new ChangePasswordRequest(PASSWORD, "brand-new-pass"));

    String hash = userRepository.findById(ana.id()).orElseThrow().getPassword();
    assertThat(passwordEncoder.matches("brand-new-pass", hash)).isTrue();
    assertThat(authService.login(new LoginRequest("ana@example.com", "brand-new-pass")))
        .isNotNull();
    LoginRequest oldPassword = new LoginRequest("ana@example.com", PASSWORD);
    assertThatThrownBy(() -> authService.login(oldPassword))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void aWrongCurrentPasswordKeepsTheOldOne() {
    UserDTO ana = registerAndLogInAna();
    ChangePasswordRequest request = new ChangePasswordRequest("wrong-pass", "brand-new-pass");

    assertThatThrownBy(() -> userService.changePassword(ana.id(), request))
        .isInstanceOf(BadRequestException.class);
    String hash = userRepository.findById(ana.id()).orElseThrow().getPassword();
    assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
  }

  @Test
  void replacingTheAvatarDeletesThePreviousImageRow() {
    UserDTO ana = registerAndLogInAna();
    ImageDTO first = userService.replaceAvatar(ana.id(), png()).image();

    ImageDTO second = userService.replaceAvatar(ana.id(), png()).image();

    assertThat(imageRepository.existsById(first.id())).isFalse();
    assertThat(imageRepository.existsById(second.id())).isTrue();
    assertThat(imageRepository.count()).isEqualTo(1);
    assertThat(userService.getProfile(ana.id()).avatarId()).isEqualTo(second.id());
  }

  @Test
  void theLoggedInUserCarriesTheAvatarIdWithoutLoadingTheImage() {
    UserDTO ana = registerAndLogInAna();
    ImageDTO avatar = userService.replaceAvatar(ana.id(), png()).image();

    // Outside any transaction: the id of the lazy avatar must still be readable.
    assertThat(userDetailsService.loadUserById(ana.id()).getAvatarId()).isEqualTo(avatar.id());
    assertThat(userService.findAvatarContent(ana.id()).contentType()).isEqualTo("image/png");
  }

  @Test
  void deletingTheAvatarClearsTheLinkAndTheImage() {
    UserDTO ana = registerAndLogInAna();
    userService.replaceAvatar(ana.id(), png());

    userService.deleteAvatar(ana.id());

    assertThat(userRepository.findById(ana.id()).orElseThrow().getAvatar()).isNull();
    assertThat(imageRepository.count()).isZero();
  }
}
