package com.miaupy.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class OpenApiDocumentationCoverageTest {
  @Test
  void everyRestEndpointAndInputHasLocalDocumentation() throws Exception {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

    for (var candidate : scanner.findCandidateComponents("com.miaupy")) {
      Class<?> controller =
          ClassUtils.forName(candidate.getBeanClassName(), getClass().getClassLoader());
      for (Method method : controller.getDeclaredMethods()) {
        if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) == null) {
          continue;
        }
        assertOperationDocumentation(controller, method);
        assertInputDocumentation(controller, method);
      }
    }
  }

  private void assertOperationDocumentation(Class<?> controller, Method method) {
    Operation operation = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class);
    String endpoint = controller.getSimpleName() + "." + method.getName();
    assertThat(operation).as("@Operation ausente em %s", endpoint).isNotNull();
    assertThat(operation.summary()).as("summary ausente em %s", endpoint).isNotBlank();
    assertThat(operation.description()).as("description ausente em %s", endpoint).isNotBlank();
  }

  private void assertInputDocumentation(Class<?> controller, Method method) {
    for (java.lang.reflect.Parameter parameter : method.getParameters()) {
      if (isHttpParameter(parameter)) {
        Parameter documentation = parameter.getAnnotation(Parameter.class);
        assertThat(documentation)
            .as(
                "@Parameter ausente em %s.%s(%s)",
                controller.getSimpleName(), method.getName(), parameter.getName())
            .isNotNull();
        assertThat(documentation.description())
            .as(
                "descrição de @Parameter ausente em %s.%s(%s)",
                controller.getSimpleName(), method.getName(), parameter.getName())
            .isNotBlank();
      }
      if (parameter.isAnnotationPresent(RequestBody.class)) {
        assertRequestSchemaDocumentation(parameter.getType());
      }
    }
  }

  private boolean isHttpParameter(java.lang.reflect.Parameter parameter) {
    return parameter.isAnnotationPresent(PathVariable.class)
        || parameter.isAnnotationPresent(RequestParam.class)
        || parameter.isAnnotationPresent(RequestHeader.class);
  }

  private void assertRequestSchemaDocumentation(Class<?> requestType) {
    assertThat(requestType.isRecord()).as("Request body deve ser record: %s", requestType).isTrue();
    for (RecordComponent component : requestType.getRecordComponents()) {
      Schema schema = component.getAccessor().getAnnotation(Schema.class);
      assertThat(schema)
          .as("@Schema ausente em %s.%s", requestType.getSimpleName(), component.getName())
          .isNotNull();
      assertThat(schema.description())
          .as(
              "descrição de @Schema ausente em %s.%s",
              requestType.getSimpleName(), component.getName())
          .isNotBlank();
    }
  }
}
