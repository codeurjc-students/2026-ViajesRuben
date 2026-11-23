package com.ruben.backend.activity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.ActivityLogEntryDTO;
import com.ruben.backend.activity.entity.ActivityLogAction;
import com.ruben.backend.activity.service.ActivityLogService;
import com.ruben.backend.common.dto.PageDTO;
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

@Tag("I4")
@ExtendWith(MockitoExtension.class)
class ActivityLogControllerUnitTest {

  @Mock private ActivityLogService activityLogService;
  @InjectMocks private ActivityLogController controller;

  @Test
  void getActivityLogReturnsThePageOfTheService() {
    Pageable pageable = PageRequest.of(0, 10);
    ActivityLogEntryDTO entry =
        new ActivityLogEntryDTO(
            1L,
            5L,
            "Colosseum",
            8L,
            "Bob",
            ActivityLogAction.UPDATED,
            List.of("name"),
            Instant.parse("2027-01-01T10:00:00Z"));
    PageDTO<ActivityLogEntryDTO> page = new PageDTO<>(List.of(entry), 0, 10, 1, true);
    when(activityLogService.findPage(3L, pageable)).thenReturn(page);

    assertThat(controller.getActivityLog(3L, pageable)).isEqualTo(page);
  }
}
