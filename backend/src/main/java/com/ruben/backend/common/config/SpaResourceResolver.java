package com.ruben.backend.common.config;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the built React application: files that exist are returned as they are and client-side
 * routes such as {@code /trips} get {@code index.html}. Unknown {@code /api/**} URLs and missing
 * files (a last segment with an extension) are not resolved, so they keep answering 404.
 */
public class SpaResourceResolver extends PathResourceResolver {

  private static final String INDEX = "index.html";

  @Override
  protected Resource getResource(String resourcePath, Resource location) throws IOException {
    Resource resource = super.getResource(resourcePath, location);
    if (resource != null || isApiPath(resourcePath) || hasExtension(resourcePath)) {
      return resource;
    }
    Resource index = location.createRelative(INDEX);
    return index.isReadable() ? index : null;
  }

  private static boolean isApiPath(String resourcePath) {
    return resourcePath.equals("api") || resourcePath.startsWith("api/");
  }

  private static boolean hasExtension(String resourcePath) {
    return resourcePath.substring(resourcePath.lastIndexOf('/') + 1).contains(".");
  }
}
