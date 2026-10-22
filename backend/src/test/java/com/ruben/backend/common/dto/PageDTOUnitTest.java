package com.ruben.backend.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageDTOUnitTest {

  @Test
  void copiesTheContentAndPositionOfAMiddlePage() {
    PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 7);

    PageDTO<String> dto = PageDTO.from(page);

    assertThat(dto.content()).containsExactly("a", "b");
    assertThat(dto.page()).isEqualTo(1);
    assertThat(dto.size()).isEqualTo(2);
    assertThat(dto.totalElements()).isEqualTo(7);
    assertThat(dto.last()).isFalse();
  }

  @Test
  void marksTheLastPage() {
    PageImpl<String> page = new PageImpl<>(List.of("g"), PageRequest.of(3, 2), 7);

    assertThat(PageDTO.from(page).last()).isTrue();
  }
}
