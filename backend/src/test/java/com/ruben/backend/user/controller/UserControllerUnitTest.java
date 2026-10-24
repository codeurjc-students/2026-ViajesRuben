package com.ruben.backend.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.user.dto.RegisterRequest;
import com.ruben.backend.user.dto.UserDTO;
import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.service.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag("B2")
@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

  @Mock private UserService userService;
  @InjectMocks private UserController controller;

  @Test
  void registerAnswers201WithTheLocationAndTheUser() {
    RegisterRequest request = new RegisterRequest("Ana", "ana@example.com", "secret-pass");
    UserDTO created = new UserDTO(5L, "Ana", "ana@example.com", Role.USER);
    when(userService.register(request)).thenReturn(created);

    ResponseEntity<UserDTO> response = controller.register(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/users/5");
    assertThat(response.getBody()).isEqualTo(created);
  }
}
