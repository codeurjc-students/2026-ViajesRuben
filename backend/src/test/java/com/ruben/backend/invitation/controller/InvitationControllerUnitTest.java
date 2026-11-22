package com.ruben.backend.invitation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.invitation.dto.InvitationDTO;
import com.ruben.backend.invitation.dto.InvitationRequest;
import com.ruben.backend.invitation.entity.InvitationStatus;
import com.ruben.backend.invitation.service.InvitationService;
import java.time.Instant;
import java.util.List;
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

@Tag("I1")
@ExtendWith(MockitoExtension.class)
class InvitationControllerUnitTest {

  private static final InvitationDTO INVITATION =
      new InvitationDTO(5L, "new@example.com", InvitationStatus.PENDING, Instant.now());

  @Mock private InvitationService invitationService;
  @InjectMocks private InvitationController controller;

  @Test
  void createInvitationAnswers201WithTheLocationAndTheInvitation() {
    InvitationRequest request = new InvitationRequest("new@example.com");
    when(invitationService.create(3L, request)).thenReturn(INVITATION);

    ResponseEntity<InvitationDTO> response = controller.createInvitation(3L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getHeaders().getLocation()).hasToString("/api/v1/trips/3/invitations/5");
    assertThat(response.getBody()).isSameAs(INVITATION);
  }

  @Test
  void getInvitationsReturnsThePageOfTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    PageDTO<InvitationDTO> page = new PageDTO<>(List.of(INVITATION), 0, 10, 1, true);
    when(invitationService.findPage(3L, pageable)).thenReturn(page);

    assertThat(controller.getInvitations(3L, pageable)).isEqualTo(page);
  }

  @Test
  void getInvitationReturnsWhatTheServiceFinds() {
    when(invitationService.findById(3L, 5L)).thenReturn(INVITATION);

    assertThat(controller.getInvitation(3L, 5L)).isSameAs(INVITATION);
  }
}
