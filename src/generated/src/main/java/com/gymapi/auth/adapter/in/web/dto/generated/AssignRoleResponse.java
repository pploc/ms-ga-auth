package com.gymapi.auth.adapter.in.web.dto.generated;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * AssignRoleResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-28T22:30:26.877429200+07:00[Asia/Bangkok]")
public class AssignRoleResponse {

  private Optional<String> message = Optional.empty();

  private Optional<UUID> userId = Optional.empty();

  private Optional<UUID> roleId = Optional.empty();

  public AssignRoleResponse message(String message) {
    this.message = Optional.of(message);
    return this;
  }

  /**
   * Get message
   * @return message
  */
  
  @Schema(name = "message", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("message")
  public Optional<String> getMessage() {
    return message;
  }

  public void setMessage(Optional<String> message) {
    this.message = message;
  }

  public AssignRoleResponse userId(UUID userId) {
    this.userId = Optional.of(userId);
    return this;
  }

  /**
   * Get userId
   * @return userId
  */
  @Valid 
  @Schema(name = "userId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("userId")
  public Optional<UUID> getUserId() {
    return userId;
  }

  public void setUserId(Optional<UUID> userId) {
    this.userId = userId;
  }

  public AssignRoleResponse roleId(UUID roleId) {
    this.roleId = Optional.of(roleId);
    return this;
  }

  /**
   * Get roleId
   * @return roleId
  */
  @Valid 
  @Schema(name = "roleId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("roleId")
  public Optional<UUID> getRoleId() {
    return roleId;
  }

  public void setRoleId(Optional<UUID> roleId) {
    this.roleId = roleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignRoleResponse assignRoleResponse = (AssignRoleResponse) o;
    return Objects.equals(this.message, assignRoleResponse.message) &&
        Objects.equals(this.userId, assignRoleResponse.userId) &&
        Objects.equals(this.roleId, assignRoleResponse.roleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, userId, roleId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignRoleResponse {\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
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

