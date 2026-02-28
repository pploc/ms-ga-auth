package com.gymapi.auth.adapter.in.web.dto.generated;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * CreatePermissionRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-28T22:30:26.877429200+07:00[Asia/Bangkok]")
public class CreatePermissionRequest {

  private String resource;

  private String action;

  private Optional<String> description = Optional.empty();

  public CreatePermissionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreatePermissionRequest(String resource, String action) {
    this.resource = resource;
    this.action = action;
  }

  public CreatePermissionRequest resource(String resource) {
    this.resource = resource;
    return this;
  }

  /**
   * Get resource
   * @return resource
  */
  @NotNull @Size(max = 50) 
  @Schema(name = "resource", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("resource")
  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public CreatePermissionRequest action(String action) {
    this.action = action;
    return this;
  }

  /**
   * Get action
   * @return action
  */
  @NotNull @Size(max = 50) 
  @Schema(name = "action", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("action")
  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public CreatePermissionRequest description(String description) {
    this.description = Optional.of(description);
    return this;
  }

  /**
   * Get description
   * @return description
  */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public Optional<String> getDescription() {
    return description;
  }

  public void setDescription(Optional<String> description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreatePermissionRequest createPermissionRequest = (CreatePermissionRequest) o;
    return Objects.equals(this.resource, createPermissionRequest.resource) &&
        Objects.equals(this.action, createPermissionRequest.action) &&
        Objects.equals(this.description, createPermissionRequest.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resource, action, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreatePermissionRequest {\n");
    sb.append("    resource: ").append(toIndentedString(resource)).append("\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

