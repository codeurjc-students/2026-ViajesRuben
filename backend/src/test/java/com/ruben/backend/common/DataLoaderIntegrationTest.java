package com.ruben.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruben.backend.DatabaseCleaner;
import com.ruben.backend.TestcontainersConfiguration;
import com.ruben.backend.trip.repository.TripRepository;
import com.ruben.backend.user.dto.LoginRequest;
import com.ruben.backend.user.repository.UserRepository;
import com.ruben.backend.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/** The sample data is off in the tests, so the loader is built by hand over the real database. */
@Tag("B4")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DataLoaderIntegrationTest {

  @Autowired private UserRepository userRepository;
  @Autowired private TripRepository tripRepository;
  @Autowired private AuthService authService;
  @Autowired private DatabaseCleaner databaseCleaner;

  private DataLoader dataLoader;

  @BeforeEach
  void setUp() {
    databaseCleaner.clear();
    dataLoader = new DataLoader(userRepository, tripRepository);
  }

  @Test
  void loadsTheSampleUsersWithTheirTripsAndRunningAgainAddsNothing() {
    dataLoader.run();
    dataLoader.run();

    assertThat(userRepository.count()).isEqualTo(3);
    assertThat(tripRepository.count()).isEqualTo(17);
    assertThat(
            tripRepository
                .findByCreatorId(
                    userRepository.findByEmail("ana@tripmate.com").orElseThrow().getId(),
                    PageRequest.of(0, 50))
                .getTotalElements())
        .isEqualTo(12);
  }

  @Test
  void theSampleUsersLogInWithTheDocumentedPassword() {
    dataLoader.run();

    for (String email :
        new String[] {"ana@tripmate.com", "carlos@tripmate.com", "laura@tripmate.com"}) {
      assertThat(authService.login(new LoginRequest(email, "Demo-2026")).user().email())
          .isEqualTo(email);
    }
  }
}
