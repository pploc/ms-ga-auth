package com.gymapi.auth.adapter.in.web.advice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymapi.auth.domain.exception.DuplicateRoleException;
import com.gymapi.auth.domain.exception.EventPublishFailedException;
import com.gymapi.auth.domain.exception.PermissionNotFoundException;
import com.gymapi.auth.domain.exception.RoleNotFoundException;
import com.gymapi.auth.domain.exception.SystemRoleModificationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Drives the advice through a stub controller so the assertions cover the wiring — argument
 * resolution, content negotiation, status selection — and not just the handler methods in
 * isolation.
 */
class GlobalExceptionHandlerTest {

  private static final Validator VALIDATOR =
      Validation.buildDefaultValidatorFactory().getValidator();

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler(new ErrorResponseFactory()))
            .build();
  }

  @Test
  void notFoundDomainExceptionBecomes404WithItsCode() throws Exception {
    mockMvc
        .perform(get("/test/throw/role-not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Role not found with id: 42"))
        .andExpect(jsonPath("$.path").value("/test/throw/role-not-found"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void conflictDomainExceptionBecomes409() throws Exception {
    mockMvc
        .perform(get("/test/throw/duplicate-role"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROLE_ALREADY_EXISTS"));
  }

  @Test
  void forbiddenDomainExceptionBecomes403() throws Exception {
    mockMvc
        .perform(get("/test/throw/system-role"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYSTEM_ROLE_IMMUTABLE"));
  }

  @Test
  void permissionNotFoundKeepsItsOwnCode() throws Exception {
    mockMvc
        .perform(get("/test/throw/permission-not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PERMISSION_NOT_FOUND"));
  }

  @Test
  void bodyValidationFailureListsEveryRejectedField() throws Exception {
    mockMvc
        .perform(
            post("/test/echo").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value("Request validation failed for 1 field"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
        .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").value(""))
        .andExpect(jsonPath("$.fieldErrors[0].message").exists());
  }

  @Test
  void validationFailureRedactsSensitiveRejectedValues() throws Exception {
    mockMvc
        .perform(
            post("/test/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ok\",\"password\":\"hunter2\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
        .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").value("[redacted]"));
  }

  @Test
  void nonUuidPathVariableBecomes400NotAnInternalError() throws Exception {
    mockMvc
        .perform(get("/test/roles/{id}", "not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.message").value("Parameter 'id' must be a valid UUID"));
  }

  @Test
  void unparseableBodyBecomes400WithoutLeakingTheParserMessage() throws Exception {
    mockMvc
        .perform(post("/test/echo").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
  }

  @Test
  void wrongMethodBecomes405() throws Exception {
    mockMvc
        .perform(post("/test/throw/role-not-found"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void wrongContentTypeBecomes415() throws Exception {
    mockMvc
        .perform(post("/test/echo").contentType(MediaType.TEXT_PLAIN).content("name=x"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @Test
  void accessDeniedBecomes403() throws Exception {
    mockMvc
        .perform(get("/test/throw/access-denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
        .andExpect(jsonPath("$.message").value("Access is denied"));
  }

  @Test
  void dataIntegrityViolationBecomes409WithoutTheDatabaseMessage() throws Exception {
    mockMvc
        .perform(get("/test/throw/data-integrity"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"))
        .andExpect(jsonPath("$.message").value("The request conflicts with existing data"));
  }

  @Test
  void unexpectedExceptionBecomes500WithAGenericMessage() throws Exception {
    mockMvc
        .perform(get("/test/throw/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(
            jsonPath("$.message")
                .value("An unexpected error occurred. Quote the traceId when reporting this."))
        // The cause belongs in the log, not in the response.
        .andExpect(
            jsonPath("$.message")
                .value(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("connection pool exhausted"))));
  }

  @Test
  void aMissingQueryParameterBecomes400() throws Exception {
    mockMvc
        .perform(get("/test/search"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.message").value("Required parameter 'q' is missing"));
  }

  @Test
  void aParameterOfTheWrongTypeNamesTheExpectedType() throws Exception {
    mockMvc
        .perform(get("/test/search").param("q", "x").param("limit", "not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.message").value("Parameter 'limit' must be a valid INTEGER"));
  }

  @Test
  void aConstraintViolationOnAParameterIsReportedPerField() throws Exception {
    mockMvc
        .perform(get("/test/throw/constraint-violation"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("size"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("must be positive"));
  }

  /** Hibernate prefixes nested paths; the caller wants the field, not the traversal. */
  @Test
  void aNestedConstraintPathIsReducedToTheField() throws Exception {
    mockMvc
        .perform(get("/test/throw/nested-constraint-violation"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
  }

  @Test
  void anAuthenticationFailureBecomes401() throws Exception {
    mockMvc
        .perform(get("/test/throw/unauthenticated"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void anUnroutableRequestBecomes404() throws Exception {
    mockMvc
        .perform(get("/test/throw/no-handler"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("No endpoint GET /test/throw/no-handler"));
  }

  @Test
  void aPublishFailureBecomes503BecauseRetryingIsTheRightAnswer() throws Exception {
    mockMvc
        .perform(get("/test/throw/publish-failed"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("EVENT_PUBLISH_FAILED"));
  }

  @Test
  void anOverlongRejectedValueIsTruncatedRatherThanEchoedWhole() throws Exception {
    String oversized = "y".repeat(500);

    mockMvc
        .perform(
            post("/test/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\"}".formatted(oversized)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.fieldErrors[0].rejectedValue").value(org.hamcrest.Matchers.hasLength(121)));
  }

  @RestController
  static class ThrowingController {

    @GetMapping("/test/search")
    void search(@RequestParam String q, @RequestParam(required = false) Integer limit) {
      // Reached only when both parameters bind.
    }

    @GetMapping("/test/throw/constraint-violation")
    void constraintViolation() {
      throw new ConstraintViolationException(VALIDATOR.validate(new SizedQuery(0)));
    }

    @GetMapping("/test/throw/nested-constraint-violation")
    void nestedConstraintViolation() {
      throw new ConstraintViolationException(
          VALIDATOR.validate(new QueryWrapper(new SizedQuery(0))));
    }

    @GetMapping("/test/throw/unauthenticated")
    void unauthenticated() {
      throw new BadCredentialsException("bad token");
    }

    @GetMapping("/test/throw/no-handler")
    void noHandler() throws NoHandlerFoundException {
      throw new NoHandlerFoundException("GET", "/test/throw/no-handler", HttpHeaders.EMPTY);
    }

    @GetMapping("/test/throw/publish-failed")
    void publishFailed() {
      throw EventPublishFailedException.of("auth.role_assigned", new IllegalStateException("down"));
    }

    @GetMapping("/test/throw/role-not-found")
    void roleNotFound() {
      throw new RoleNotFoundException("Role not found with id: 42");
    }

    @GetMapping("/test/throw/duplicate-role")
    void duplicateRole() {
      throw DuplicateRoleException.byName("MEMBER");
    }

    @GetMapping("/test/throw/system-role")
    void systemRole() {
      throw SystemRoleModificationException.cannotDelete("MEMBER");
    }

    @GetMapping("/test/throw/permission-not-found")
    void permissionNotFound() {
      throw PermissionNotFoundException.byResourceAndAction("booking", "create");
    }

    @GetMapping("/test/throw/access-denied")
    void accessDenied() {
      throw new AccessDeniedException("nope");
    }

    @GetMapping("/test/throw/data-integrity")
    void dataIntegrity() {
      throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
    }

    @GetMapping("/test/throw/boom")
    void boom() {
      throw new IllegalStateException("connection pool exhausted");
    }

    @GetMapping("/test/roles/{id}")
    void byId(@PathVariable UUID id) {
      // Reached only when the id parses.
    }

    @PostMapping("/test/echo")
    void echo(@Valid @RequestBody Payload payload) {
      // Reached only when the payload validates.
    }
  }

  /** Records deserialize and validate exactly like a bean, with a lot less ceremony. */
  record Payload(@NotBlank @Size(max = 50) String name, @Size(max = 3) String password) {}

  record SizedQuery(@Positive(message = "must be positive") int size) {}

  /** Nested so the property path has a prefix the handler has to strip. */
  record QueryWrapper(@Valid SizedQuery query) {}
}
