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
 * UserRoleResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-28T22:30:26.877429200+07:00[Asia/Bangkok]")
public class UserRoleResponse {

  private Optional<UUID> id = Optional.empty();

  private Optional<UUID> userId = Optional.empty();

  private Optional<UUID> roleId = Optional.empty();

  private Optional<String> roleName = Optional.empty();

  private Optional<UUID> assignedBy = Optional.empty();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Optional<OffsetDateTime> assignedAt = Optional.empty();

  public UserRoleResponse id(UUID id) {
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

  public UserRoleResponse userId(UUID userId) {
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

  public UserRoleResponse roleId(UUID roleId) {
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

  public UserRoleResponse roleName(String roleName) {
    this.roleName = Optional.of(roleName);
    return this;
  }

  /**
   * Get roleName
   * @return roleName
  */
  
  @Schema(name = "roleName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("roleName")
  public Optional<String> getRoleName() {
    return roleName;
  }

  public void setRoleName(Optional<String> roleName) {
    this.roleName = roleName;
  }

  public UserRoleResponse assignedBy(UUID assignedBy) {
    this.assignedBy = Optional.of(assignedBy);
    return this;
  }

  /**
   * Get assignedBy
   * @return assignedBy
  */
  @Valid 
  @Schema(name = "assignedBy", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("assignedBy")
  public Optional<UUID> getAssignedBy() {
    return assignedBy;
  }

  public void setAssignedBy(Optional<UUID> assignedBy) {
    this.assignedBy = assignedBy;
  }

  public UserRoleResponse assignedAt(OffsetDateTime assignedAt) {
    this.assignedAt = Optional.of(assignedAt);
    return this;
  }

  /**
   * Get assignedAt
   * @return assignedAt
  */
  @Valid 
  @Schema(name = "assignedAt", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("assignedAt")
  public Optional<OffsetDateTime> getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(Optional<OffsetDateTime> assignedAt) {
    this.assignedAt = assignedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserRoleResponse userRoleResponse = (UserRoleResponse) o;
    return Objects.equals(this.id, userRoleResponse.id) &&
        Objects.equals(this.userId, userRoleResponse.userId) &&
        Objects.equals(this.roleId, userRoleResponse.roleId) &&
        Objects.equals(this.roleName, userRoleResponse.roleName) &&
        Objects.equals(this.assignedBy, userRoleResponse.assignedBy) &&
        Objects.equals(this.assignedAt, userRoleResponse.assignedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userId, roleId, roleName, assignedBy, assignedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserRoleResponse {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
    sb.append("    roleName: ").append(toIndentedString(roleName)).append("\n");
    sb.append("    assignedBy: ").append(toIndentedString(assignedBy)).append("\n");
    sb.append("    assignedAt: ").append(toIndentedString(assignedAt)).append("\n");
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

