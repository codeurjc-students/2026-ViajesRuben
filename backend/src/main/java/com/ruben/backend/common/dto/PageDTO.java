package com.ruben.backend.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Body of every paginated list: the items of one page plus what the client needs to ask for the
 * next.
 */
public record PageDTO<T>(List<T> content, int page, int size, long totalElements, boolean last) {

  public static <T> PageDTO<T> from(Page<T> page) {
    return new PageDTO<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.isLast());
  }
}
