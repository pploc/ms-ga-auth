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
 * AssignRoleRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-28T22:30:26.877429200+07:00[Asia/Bangkok]")
public class AssignRoleRequest {

  private UUID roleId;

  private Optional<UUID> assignedBy = Optional.empty();

  public AssignRoleRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AssignRoleRequest(UUID roleId) {
    this.roleId = roleId;
  }

  public AssignRoleRequest roleId(UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Get roleId
   * @return roleId
  */
  @NotNull @Valid 
  @Schema(name = "roleId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("roleId")
  public UUID getRoleId() {
    return roleId;
  }

  public void setRoleId(UUID roleId) {
    this.roleId = roleId;
  }

  public AssignRoleRequest assignedBy(UUID assignedBy) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignRoleRequest assignRoleRequest = (AssignRoleRequest) o;
    return Objects.equals(this.roleId, assignRoleRequest.roleId) &&
        Objects.equals(this.assignedBy, assignRoleRequest.assignedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, assignedBy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignRoleRequest {\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
    sb.append("    assignedBy: ").append(toIndentedString(assignedBy)).append("\n");
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

