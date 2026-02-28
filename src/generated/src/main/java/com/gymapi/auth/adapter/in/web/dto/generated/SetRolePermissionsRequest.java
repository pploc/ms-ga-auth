package com.gymapi.auth.adapter.in.web.dto.generated;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * SetRolePermissionsRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-28T22:30:26.877429200+07:00[Asia/Bangkok]")
public class SetRolePermissionsRequest {

  @Valid
  private List<UUID> permissionIds = new ArrayList<>();

  public SetRolePermissionsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SetRolePermissionsRequest(List<UUID> permissionIds) {
    this.permissionIds = permissionIds;
  }

  public SetRolePermissionsRequest permissionIds(List<UUID> permissionIds) {
    this.permissionIds = permissionIds;
    return this;
  }

  public SetRolePermissionsRequest addPermissionIdsItem(UUID permissionIdsItem) {
    if (this.permissionIds == null) {
      this.permissionIds = new ArrayList<>();
    }
    this.permissionIds.add(permissionIdsItem);
    return this;
  }

  /**
   * Get permissionIds
   * @return permissionIds
  */
  @NotNull @Valid 
  @Schema(name = "permissionIds", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("permissionIds")
  public List<UUID> getPermissionIds() {
    return permissionIds;
  }

  public void setPermissionIds(List<UUID> permissionIds) {
    this.permissionIds = permissionIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetRolePermissionsRequest setRolePermissionsRequest = (SetRolePermissionsRequest) o;
    return Objects.equals(this.permissionIds, setRolePermissionsRequest.permissionIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permissionIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetRolePermissionsRequest {\n");
    sb.append("    permissionIds: ").append(toIndentedString(permissionIds)).append("\n");
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

