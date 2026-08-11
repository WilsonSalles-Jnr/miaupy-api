package com.miaupy.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

class OpenApiDocumentationCoverageTest {
  @Test
  void everyRestEndpointHasExplicitSummaryAndDescription() throws Exception {
    Set<String> actualEndpoints = scanEndpoints();

    assertThat(OpenApiDocumentationCustomizer.documentedEndpoints().keySet())
        .containsExactlyInAnyOrderElementsOf(actualEndpoints);
    assertThat(OpenApiDocumentationCustomizer.documentedEndpoints().values())
        .allSatisfy(
            doc -> {
              assertThat(doc.summary()).isNotBlank();
              assertThat(doc.description()).isNotBlank();
            });
  }

  private Set<String> scanEndpoints() throws Exception {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    Set<String> endpoints = new LinkedHashSet<>();
    for (var candidate : scanner.findCandidateComponents("com.miaupy")) {
      Class<?> controller =
          ClassUtils.forName(candidate.getBeanClassName(), getClass().getClassLoader());
      String base = firstPath(controller.getAnnotation(RequestMapping.class));
      for (Method method : controller.getDeclaredMethods()) {
        RequestMapping mapping =
            org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation(
                method, RequestMapping.class);
        if (mapping == null) {
          continue;
        }
        String path = normalize(base + firstPath(mapping));
        for (RequestMethod requestMethod : mapping.method()) {
          endpoints.add(requestMethod.name() + " " + path);
        }
      }
    }
    return endpoints;
  }

  private String firstPath(RequestMapping mapping) {
    if (mapping == null) {
      return "";
    }
    if (mapping.path().length > 0) {
      return mapping.path()[0];
    }
    return mapping.value().length > 0 ? mapping.value()[0] : "";
  }

  private String normalize(String path) {
    return path.replaceAll("//+", "/");
  }
}
