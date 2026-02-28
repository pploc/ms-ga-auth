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
 * RoleResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-28T22:30:26.877429200+07:00[Asia/Bangkok]")
public class RoleResponse {

  private Optional<UUID> id = Optional.empty();

  private Optional<String> name = Optional.empty();

  private Optional<String> description = Optional.empty();

  private Optional<Boolean> isSystem = Optional.empty();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Optional<OffsetDateTime> createdAt = Optional.empty();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Optional<OffsetDateTime> updatedAt = Optional.empty();

  public RoleResponse id(UUID id) {
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

  public RoleResponse name(String name) {
    this.name = Optional.of(name);
    return this;
  }

  /**
   * Get name
   * @return name
  */
  
  @Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public Optional<String> getName() {
    return name;
  }

  public void setName(Optional<String> name) {
    this.name = name;
  }

  public RoleResponse description(String description) {
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

  public RoleResponse isSystem(Boolean isSystem) {
    this.isSystem = Optional.of(isSystem);
    return this;
  }

  /**
   * Get isSystem
   * @return isSystem
  */
  
  @Schema(name = "isSystem", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("isSystem")
  public Optional<Boolean> getIsSystem() {
    return isSystem;
  }

  public void setIsSystem(Optional<Boolean> isSystem) {
    this.isSystem = isSystem;
  }

  public RoleResponse createdAt(OffsetDateTime createdAt) {
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

  public RoleResponse updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = Optional.of(updatedAt);
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
  */
  @Valid 
  @Schema(name = "updatedAt", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("updatedAt")
  public Optional<OffsetDateTime> getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Optional<OffsetDateTime> updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoleResponse roleResponse = (RoleResponse) o;
    return Objects.equals(this.id, roleResponse.id) &&
        Objects.equals(this.name, roleResponse.name) &&
        Objects.equals(this.description, roleResponse.description) &&
        Objects.equals(this.isSystem, roleResponse.isSystem) &&
        Objects.equals(this.createdAt, roleResponse.createdAt) &&
        Objects.equals(this.updatedAt, roleResponse.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, isSystem, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoleResponse {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    isSystem: ").append(toIndentedString(isSystem)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

