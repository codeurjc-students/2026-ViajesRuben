package com.ruben.backend.activity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.activity.dto.TripConflictsDTO;
import com.ruben.backend.activity.service.ConflictService;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("B6")
@ExtendWith(MockitoExtension.class)
class ConflictControllerUnitTest {

  @Mock private ConflictService conflictService;
  @InjectMocks private ConflictController controller;

  @Test
  void getConflictsReturnsWhatTheServiceFindsForTheTrip() {
    TripConflictsDTO conflicts = new TripConflictsDTO(List.of(), List.of(), null);
    when(conflictService.findConflicts(3L)).thenReturn(conflicts);

    assertThat(controller.getConflicts(3L)).isSameAs(conflicts);
  }
}
