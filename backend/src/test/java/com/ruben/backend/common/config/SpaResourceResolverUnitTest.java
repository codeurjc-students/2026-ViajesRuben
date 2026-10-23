package com.ruben.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Tag("docker")
class SpaResourceResolverUnitTest {

  private final SpaResourceResolver resolver = new SpaResourceResolver();

  @TempDir Path root;

  private Resource location;

  @BeforeEach
  void setUp() throws IOException {
    Files.writeString(root.resolve("index.html"), "<html></html>");
    Files.createDirectories(root.resolve("assets"));
    Files.writeString(root.resolve("assets/app.js"), "console.log(1);");
    location = new FileSystemResource(root + "/");
  }

  @Test
  void anExistingFileIsReturnedAsItIs() throws IOException {
    Resource resource = resolver.getResource("assets/app.js", location);

    assertThat(resource).isNotNull();
    assertThat(resource.getFilename()).isEqualTo("app.js");
  }

  @Test
  void aClientRouteGetsIndexHtml() throws IOException {
    assertThat(resolver.getResource("trips", location).getFilename()).isEqualTo("index.html");
    assertThat(resolver.getResource("trips/12/edit", location).getFilename())
        .isEqualTo("index.html");
  }

  @Test
  void anUnknownApiUrlIsNotResolved() throws IOException {
    assertThat(resolver.getResource("api/v1/unknown", location)).isNull();
    assertThat(resolver.getResource("api", location)).isNull();
  }

  @Test
  void aMissingFileIsNotResolved() throws IOException {
    assertThat(resolver.getResource("missing.png", location)).isNull();
    assertThat(resolver.getResource("assets/missing.js", location)).isNull();
  }

  @Test
  void aClientRouteIsNotResolvedWhenThereIsNoIndexHtml(@TempDir Path emptyRoot) throws IOException {
    Resource emptyLocation = new FileSystemResource(emptyRoot + "/");

    assertThat(resolver.getResource("trips", emptyLocation)).isNull();
  }
}
