package com.gymapi.auth.adapter.in.web.openapi;

import com.gymapi.auth.adapter.in.web.dto.generated.ErrorResponse;
import com.gymapi.auth.adapter.in.web.filter.IdempotencyFilter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Documents the errors every endpoint can return, without repeating four {@code @ApiResponse}
 * annotations on all seventeen controller methods.
 *
 * <p>Operation-specific failures (a 404 for a missing role, a 409 for a duplicate name) stay on the
 * controller method — only the universal ones are filled in here, and never over an entry the
 * controller already declared.
 */
@Component
public class ErrorResponsesCustomizer implements OpenApiCustomizer {

  private static final String JSON = "application/json";
  private static final String CORRELATION_HEADER = "X-Correlation-Id";

  /** Reusable {@code components.responses} entries, keyed by the status they document. */
  private static final Map<String, String> SHARED_RESPONSES =
      Map.of(
          "400", "BadRequest",
          "401", "Unauthorized",
          "403", "Forbidden",
          "500", "InternalServerError");

  /** Methods that accept an {@code Idempotency-Key}; the safe ones need no such thing. */
  private static final Set<PathItem.HttpMethod> GUARDED_METHODS =
      EnumSet.of(
          PathItem.HttpMethod.POST,
          PathItem.HttpMethod.PUT,
          PathItem.HttpMethod.PATCH,
          PathItem.HttpMethod.DELETE);

  @Override
  public void customise(OpenAPI openApi) {
    Components components = openApi.getComponents();
    if (components == null) {
      components = new Components();
      openApi.setComponents(components);
    }
    registerErrorSchemas(components);
    registerSharedResponses(components);

    if (openApi.getPaths() == null) {
      return;
    }
    openApi
        .getPaths()
        .values()
        .forEach(
            pathItem ->
                pathItem
                    .readOperationsMap()
                    .forEach((method, operation) -> decorate(method, operation)));
  }

  private void decorate(PathItem.HttpMethod method, Operation operation) {
    ApiResponses responses = operation.getResponses();
    if (responses == null) {
      return;
    }
    SHARED_RESPONSES.forEach(
        (status, componentName) -> {
          if (responses.get(status) == null) {
            responses.addApiResponse(status, refTo(componentName));
          }
        });
    responses.values().forEach(ErrorResponsesCustomizer::addCorrelationHeader);

    if (GUARDED_METHODS.contains(method)) {
      addIdempotencyKeyParameter(operation);
    }
  }

  /**
   * Documents the opt-in retry header on every mutating operation. Doing it here rather than with
   * an {@code @Parameter} on each controller method keeps the wording in one place.
   */
  private void addIdempotencyKeyParameter(Operation operation) {
    boolean alreadyDocumented =
        operation.getParameters() != null
            && operation.getParameters().stream()
                .anyMatch(parameter -> IdempotencyFilter.HEADER_NAME.equals(parameter.getName()));
    if (alreadyDocumented) {
      return;
    }
    operation.addParametersItem(
        new HeaderParameter()
            .name(IdempotencyFilter.HEADER_NAME)
            .required(false)
            .description(
                """
                Opaque, caller-generated key (1-255 printable ASCII characters, no whitespace) \
                that makes this request safe to retry. The first request with a given key runs \
                normally and its response is recorded; a retry carrying the same key and body \
                replays that response with `Idempotency-Replayed: true` instead of repeating the \
                work. Reusing a key for a different request is rejected with \
                `IDEMPOTENCY_KEY_REUSED`, and retrying while the first attempt is still running \
                with `IDEMPOTENT_REQUEST_IN_PROGRESS`.""")
            .schema(new StringSchema().minLength(1).maxLength(255))
            .example("3f2504e0-4f89-11d3-9a0c-0305e82c3301"));
  }

  private void registerErrorSchemas(Components components) {
    ModelConverters.getInstance().readAll(ErrorResponse.class).forEach(components::addSchemas);
  }

  private void registerSharedResponses(Components components) {
    components.addResponses(
        "BadRequest",
        errorResponse(
            """
            The request could not be processed as sent. `VALIDATION_FAILED` means a body field \
            broke a constraint — see `fieldErrors` for the specific fields. `MALFORMED_REQUEST` \
            means unparseable JSON, or a path/query value of the wrong type such as a non-UUID id."""));
    components.addResponses(
        "Unauthorized",
        errorResponse("Missing, malformed or expired bearer token (`UNAUTHENTICATED`)."));
    components.addResponses(
        "Forbidden",
        errorResponse(
            """
            The caller is authenticated but not allowed to perform the operation \
            (`ACCESS_DENIED`), or the target is a system role that is read-only through this API \
            (`SYSTEM_ROLE_IMMUTABLE`)."""));
    components.addResponses(
        "InternalServerError",
        errorResponse(
            """
            An unhandled failure. The `message` is deliberately generic — the cause is logged \
            against the `traceId`, which is what to quote when reporting the problem."""));
  }

  private static ApiResponse errorResponse(String description) {
    Schema<?> schema = new Schema<>().$ref("#/components/schemas/ErrorResponse");
    ApiResponse response =
        new ApiResponse()
            .description(description)
            .content(new Content().addMediaType(JSON, new MediaType().schema(schema)));
    addCorrelationHeader(response);
    return response;
  }

  private static ApiResponse refTo(String componentName) {
    return new ApiResponse().$ref("#/components/responses/" + componentName);
  }

  private static void addCorrelationHeader(ApiResponse response) {
    // A $ref response carries no fields of its own; the referenced component has the header.
    if (response.get$ref() != null) {
      return;
    }
    Map<String, Header> headers =
        response.getHeaders() == null ? new LinkedHashMap<>() : response.getHeaders();
    headers.computeIfAbsent(
        CORRELATION_HEADER,
        name ->
            new Header()
                .description(
                    "Correlation id for this request. Echoed from the inbound header when present,"
                        + " otherwise generated. Matches the `traceId` of error bodies.")
                .schema(new StringSchema()));
    response.setHeaders(headers);
  }
}
