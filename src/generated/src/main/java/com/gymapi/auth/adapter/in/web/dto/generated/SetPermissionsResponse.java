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
 * SetPermissionsResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-28T22:30:26.877429200+07:00[Asia/Bangkok]")
public class SetPermissionsResponse {

  private Optional<String> message = Optional.empty();

  private Optional<Integer> permissionCount = Optional.empty();

  public SetPermissionsResponse message(String message) {
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

  public SetPermissionsResponse permissionCount(Integer permissionCount) {
    this.permissionCount = Optional.of(permissionCount);
    return this;
  }

  /**
   * Get permissionCount
   * @return permissionCount
  */
  
  @Schema(name = "permissionCount", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("permissionCount")
  public Optional<Integer> getPermissionCount() {
    return permissionCount;
  }

  public void setPermissionCount(Optional<Integer> permissionCount) {
    this.permissionCount = permissionCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetPermissionsResponse setPermissionsResponse = (SetPermissionsResponse) o;
    return Objects.equals(this.message, setPermissionsResponse.message) &&
        Objects.equals(this.permissionCount, setPermissionsResponse.permissionCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, permissionCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetPermissionsResponse {\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    permissionCount: ").append(toIndentedString(permissionCount)).append("\n");
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

