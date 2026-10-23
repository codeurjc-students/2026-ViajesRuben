package com.ruben.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Tag("docker")
class SpaWebConfigUnitTest {

  @Test
  void itRegistersTheStaticResourceHandlerForEveryPath() {
    ResourceHandlerRegistry registry =
        new ResourceHandlerRegistry(new GenericWebApplicationContext(), new MockServletContext());

    new SpaWebConfig().addResourceHandlers(registry);

    assertThat(registry.hasMappingForPattern("/**")).isTrue();
  }
}
