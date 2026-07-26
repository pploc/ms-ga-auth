package com.gymapi.auth.component;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymapi.auth.adapter.in.web.filter.CorrelationIdFilter;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Every one of these used to be a 500 or an HTML whitelabel page. They are asserted end to end
 * because the fix lives in the filter chain and the advice, not in any controller.
 */
@DisplayName("Error envelope, end to end")
class ErrorContractComponentTest extends ComponentTestBase {

  @Test
  void everyResponseCarriesAGeneratedCorrelationId() throws Exception {
    mockMvc
        .perform(get("/auth/roles"))
        .andExpect(status().isOk())
        .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME));
  }

  @Test
  void anInboundCorrelationIdIsPropagatedAndReportedAsTheTraceId() throws Exception {
    mockMvc
        .perform(
            get("/auth/roles/{id}", UUID.randomUUID())
                .header(CorrelationIdFilter.HEADER_NAME, "caller-supplied-id"))
        .andExpect(status().isNotFound())
        .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "caller-supplied-id"))
        .andExpect(jsonPath("$.traceId").value("caller-supplied-id"));
  }

  @Test
  void anUnknownEndpointIsAJsonNotFound() throws Exception {
    mockMvc
        .perform(get("/auth/nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void anUnsupportedMethodIs405() throws Exception {
    mockMvc
        .perform(patch("/auth/roles").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void aNonJsonBodyIs415() throws Exception {
    mockMvc
        .perform(post("/auth/roles").contentType(MediaType.TEXT_PLAIN).content("name=X"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @Test
  void unparseableJsonIs400AndDoesNotLeakTheParserMessage() throws Exception {
    mockMvc
        .perform(post("/auth/roles").contentType(MediaType.APPLICATION_JSON).content("{oops"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
  }

  @Test
  void aPathVariableThatIsNotAUuidIs400() throws Exception {
    mockMvc
        .perform(get("/auth/roles/{id}", "not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.message").value(Matchers.containsString("must be a valid UUID")));
  }

  @Test
  void theEnvelopeIsTheSameShapeForEveryFailure() throws Exception {
    mockMvc
        .perform(get("/auth/permissions/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.code").value("PERMISSION_NOT_FOUND"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.path").value(Matchers.startsWith("/auth/permissions/")))
        .andExpect(jsonPath("$.traceId").exists())
        // Only validation failures carry field detail; it is omitted rather than sent as null.
        .andExpect(jsonPath("$.fieldErrors").doesNotExist());
  }

  @Test
  void validationFailuresListEveryOffendingField() throws Exception {
    mockMvc
        .perform(
            post("/auth/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"description": "missing resource and action"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value("Request validation failed for 2 fields"))
        .andExpect(jsonPath("$.fieldErrors.length()").value(2))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("action"))
        .andExpect(jsonPath("$.fieldErrors[1].field").value("resource"));
  }

  @Test
  void theApiDocumentIsServedAndDescribesTheErrorEnvelope() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists())
        .andExpect(jsonPath("$.components.responses.BadRequest").exists())
        .andExpect(jsonPath("$.components.responses.Unauthorized").exists())
        .andExpect(jsonPath("$.components.responses.InternalServerError").exists())
        // The customizer fills the universal errors in on operations that do not declare them.
        .andExpect(jsonPath("$.paths['/auth/roles'].get.responses.500").exists())
        // And documents the retry header on every mutating operation.
        .andExpect(
            jsonPath("$.paths['/auth/roles'].post.parameters[?(@.name == 'Idempotency-Key')]")
                .exists());
  }
}
