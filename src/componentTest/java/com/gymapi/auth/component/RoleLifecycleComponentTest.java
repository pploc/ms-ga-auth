package com.gymapi.auth.component;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Role management, end to end")
class RoleLifecycleComponentTest extends ComponentTestBase {

  @Test
  void createsReadsUpdatesAndDeletesARole() throws Exception {
    UUID roleId = createRole("FRONT_DESK");

    mockMvc
        .perform(get("/auth/roles/{id}", roleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("FRONT_DESK"))
        .andExpect(jsonPath("$.isSystem").value(false))
        .andExpect(jsonPath("$.createdAt").exists());

    mockMvc
        .perform(
            put("/auth/roles/{id}", roleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "RECEPTION", "description": "renamed"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("RECEPTION"))
        .andExpect(jsonPath("$.description").value("renamed"));

    mockMvc.perform(get("/auth/roles/name/{name}", "RECEPTION")).andExpect(status().isOk());

    mockMvc.perform(delete("/auth/roles/{id}", roleId)).andExpect(status().isNoContent());
    mockMvc
        .perform(get("/auth/roles/{id}", roleId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"));
  }

  @Test
  void listsEveryRole() throws Exception {
    createRole("ONE");
    createRole("TWO");

    mockMvc
        .perform(get("/auth/roles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void rejectsADuplicateName() throws Exception {
    createRole("DUPLICATE");

    mockMvc
        .perform(
            post("/auth/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "DUPLICATE"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROLE_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.path").value("/auth/roles"))
        .andExpect(jsonPath("$.traceId").exists());
  }

  @Test
  void rejectsABlankNameWithFieldLevelDetail() throws Exception {
    mockMvc
        .perform(
            post("/auth/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": ""}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
        .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").value(""));
  }

  @Test
  void rejectsANameLongerThanTheColumn() throws Exception {
    mockMvc
        .perform(
            post("/auth/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "%s"}
                    """
                        .formatted("X".repeat(51))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
  }

  @Test
  void refusesToMutateASystemRole() throws Exception {
    UUID systemRoleId = createRole("MEMBER", true);

    mockMvc
        .perform(
            put("/auth/roles/{id}", systemRoleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "RENAMED"}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYSTEM_ROLE_IMMUTABLE"));

    mockMvc
        .perform(delete("/auth/roles/{id}", systemRoleId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYSTEM_ROLE_IMMUTABLE"));

    mockMvc
        .perform(
            put("/auth/roles/{id}/permissions", systemRoleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"permissionIds": []}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYSTEM_ROLE_IMMUTABLE"));
  }

  @Test
  void replacesRolePermissionsRatherThanAccumulatingThem() throws Exception {
    UUID roleId = createRole("TRAINER_ASSISTANT");
    UUID read = createPermission("booking", "read");
    UUID create = createPermission("booking", "create");

    mockMvc
        .perform(
            put("/auth/roles/{id}/permissions", roleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"permissionIds": ["%s", "%s"]}
                    """
                        .formatted(read, create)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Permissions updated."))
        .andExpect(jsonPath("$.permissionCount").value(2));

    // A second call with one id replaces the set; it does not add to it.
    mockMvc
        .perform(
            put("/auth/roles/{id}/permissions", roleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"permissionIds": ["%s"]}
                    """
                        .formatted(read)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissionCount").value(1));

    mockMvc
        .perform(get("/auth/roles/{id}/permissions", roleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].resource").value("booking"))
        .andExpect(jsonPath("$[0].action").value("read"));
  }

  @Test
  void revokesEverythingWhenGivenAnEmptyPermissionSet() throws Exception {
    UUID roleId = createRole("TEMP");
    UUID permissionId = createPermission("exercise", "read");
    grantPermissions(roleId, permissionId);

    mockMvc
        .perform(
            put("/auth/roles/{id}/permissions", roleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"permissionIds": []}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissionCount").value(0));
  }

  @Test
  void reportsAnUnknownPermissionIdAsNotFoundRatherThanAServerError() throws Exception {
    UUID roleId = createRole("SOME_ROLE");

    mockMvc
        .perform(
            put("/auth/roles/{id}/permissions", roleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"permissionIds": ["%s"]}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PERMISSION_NOT_FOUND"));
  }

  @Test
  void reportsAnUnknownRoleForEveryRoleScopedRead() throws Exception {
    UUID unknown = UUID.randomUUID();

    mockMvc
        .perform(get("/auth/roles/{id}/permissions", unknown))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"));

    mockMvc
        .perform(get("/auth/roles/name/{name}", "NO_SUCH_ROLE"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"));

    mockMvc
        .perform(
            put("/auth/roles/{id}", unknown)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "WHATEVER"}
                    """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"));

    mockMvc
        .perform(delete("/auth/roles/{id}", unknown))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"));
  }

  @Test
  void rejectsRenamingOntoAnExistingName() throws Exception {
    createRole("TAKEN");
    UUID roleId = createRole("FREE");

    mockMvc
        .perform(
            put("/auth/roles/{id}", roleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "TAKEN"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ROLE_ALREADY_EXISTS"));
  }
}
