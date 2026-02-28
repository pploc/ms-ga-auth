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
 * CreateRoleRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-28T22:30:26.877429200+07:00[Asia/Bangkok]")
public class CreateRoleRequest {

  private String name;

  private Optional<String> description = Optional.empty();

  private Optional<Boolean> system = Optional.empty();

  public CreateRoleRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateRoleRequest(String name) {
    this.name = name;
  }

  public CreateRoleRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  @NotNull @Size(max = 50) 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CreateRoleRequest description(String description) {
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

  public CreateRoleRequest system(Boolean system) {
    this.system = Optional.of(system);
    return this;
  }

  /**
   * Get system
   * @return system
  */
  
  @Schema(name = "system", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("system")
  public Optional<Boolean> getSystem() {
    return system;
  }

  public void setSystem(Optional<Boolean> system) {
    this.system = system;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateRoleRequest createRoleRequest = (CreateRoleRequest) o;
    return Objects.equals(this.name, createRoleRequest.name) &&
        Objects.equals(this.description, createRoleRequest.description) &&
        Objects.equals(this.system, createRoleRequest.system);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, system);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateRoleRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    system: ").append(toIndentedString(system)).append("\n");
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

