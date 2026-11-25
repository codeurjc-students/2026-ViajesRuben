package com.ruben.backend.metrics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ruben.backend.metrics.dto.PlatformMetricsDTO;
import com.ruben.backend.metrics.service.MetricsService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("I6")
@ExtendWith(MockitoExtension.class)
class AdminMetricsControllerUnitTest {

  @Mock private MetricsService metricsService;
  @InjectMocks private AdminMetricsController controller;

  @Test
  void getMetricsReturnsTheTotalsOfTheService() {
    PlatformMetricsDTO metrics = new PlatformMetricsDTO(3L, 5L, 12L);
    when(metricsService.getPlatformMetrics()).thenReturn(metrics);

    assertThat(controller.getMetrics()).isEqualTo(metrics);
  }
}
