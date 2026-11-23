package com.ruben.backend.participation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.participation.dto.ParticipantDTO;
import com.ruben.backend.participation.service.ParticipantService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag("I3")
@ExtendWith(MockitoExtension.class)
class ParticipantControllerUnitTest {

  @Mock private ParticipantService participantService;
  @InjectMocks private ParticipantController controller;

  @Test
  void getParticipantsReturnsThePageOfTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    ParticipantDTO bob =
        new ParticipantDTO(
            8L, "Bob", UUID.randomUUID(), null, Instant.parse("2027-01-01T10:00:00Z"));
    PageDTO<ParticipantDTO> page = new PageDTO<>(List.of(bob), 0, 10, 1, true);
    when(participantService.findPage(3L, pageable)).thenReturn(page);

    assertThat(controller.getParticipants(3L, pageable)).isEqualTo(page);
  }

  @Test
  void removeParticipantAnswers204AfterTheServiceRemoves() {
    ResponseEntity<Void> response = controller.removeParticipant(3L, 8L);

    verify(participantService).remove(3L, 8L);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }
}
