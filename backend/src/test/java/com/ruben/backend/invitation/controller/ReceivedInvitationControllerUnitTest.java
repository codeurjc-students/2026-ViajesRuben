package com.ruben.backend.invitation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.common.dto.PageDTO;
import com.ruben.backend.invitation.dto.InvitationResponseRequest;
import com.ruben.backend.invitation.dto.ReceivedInvitationDTO;
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

@Tag("I2")
@ExtendWith(MockitoExtension.class)
class ReceivedInvitationControllerUnitTest {

  private static final ReceivedInvitationDTO INVITATION =
      new ReceivedInvitationDTO(
          5L, 3L, "Rome", "Ana", InvitationStatus.PENDING, Instant.now(), null);

  @Mock private InvitationService invitationService;
  @InjectMocks private ReceivedInvitationController controller;

  @Test
  void getMyInvitationsReturnsThePageOfTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    PageDTO<ReceivedInvitationDTO> page = new PageDTO<>(List.of(INVITATION), 0, 10, 1, true);
    when(invitationService.findMine(pageable)).thenReturn(page);

    assertThat(controller.getMyInvitations(pageable)).isEqualTo(page);
  }

  @Test
  void getInvitationByTokenReturnsWhatTheServiceFinds() {
    when(invitationService.findByToken("abc")).thenReturn(INVITATION);

    assertThat(controller.getInvitationByToken("abc")).isSameAs(INVITATION);
  }

  @Test
  void respondPassesTheAnswerToTheService() {
    when(invitationService.respond(5L, InvitationStatus.ACCEPTED)).thenReturn(INVITATION);

    assertThat(controller.respond(5L, new InvitationResponseRequest(InvitationStatus.ACCEPTED)))
        .isSameAs(INVITATION);
  }
}
