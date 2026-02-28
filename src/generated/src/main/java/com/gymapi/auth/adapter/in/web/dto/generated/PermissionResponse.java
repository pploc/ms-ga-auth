package com.gymapi.auth.adapter.in.web.dto.generated;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * PermissionResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-28T22:30:26.877429200+07:00[Asia/Bangkok]")
public class PermissionResponse {

  private Optional<UUID> id = Optional.empty();

  private Optional<String> resource = Optional.empty();

  private Optional<String> action = Optional.empty();

  private Optional<String> description = Optional.empty();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Optional<OffsetDateTime> createdAt = Optional.empty();

  public PermissionResponse id(UUID id) {
    this.id = Optional.of(id);
    return this;
  }

  /**
   * Get id
   * @return id
  */
  @Valid 
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Optional<UUID> getId() {
    return id;
  }

  public void setId(Optional<UUID> id) {
    this.id = id;
  }

  public PermissionResponse resource(String resource) {
    this.resource = Optional.of(resource);
    return this;
  }

  /**
   * Get resource
   * @return resource
  */
  
  @Schema(name = "resource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("resource")
  public Optional<String> getResource() {
    return resource;
  }

  public void setResource(Optional<String> resource) {
    this.resource = resource;
  }

  public PermissionResponse action(String action) {
    this.action = Optional.of(action);
    return this;
  }

  /**
   * Get action
   * @return action
  */
  
  @Schema(name = "action", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("action")
  public Optional<String> getAction() {
    return action;
  }

  public void setAction(Optional<String> action) {
    this.action = action;
  }

  public PermissionResponse description(String description) {
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

  public PermissionResponse createdAt(OffsetDateTime createdAt) {
    this.createdAt = Optional.of(createdAt);
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
  */
  @Valid 
  @Schema(name = "createdAt", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdAt")
  public Optional<OffsetDateTime> getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Optional<OffsetDateTime> createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PermissionResponse permissionResponse = (PermissionResponse) o;
    return Objects.equals(this.id, permissionResponse.id) &&
        Objects.equals(this.resource, permissionResponse.resource) &&
        Objects.equals(this.action, permissionResponse.action) &&
        Objects.equals(this.description, permissionResponse.description) &&
        Objects.equals(this.createdAt, permissionResponse.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, resource, action, description, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PermissionResponse {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    resource: ").append(toIndentedString(resource)).append("\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

