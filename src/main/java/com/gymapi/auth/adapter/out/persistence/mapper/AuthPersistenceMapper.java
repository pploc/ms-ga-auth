package com.gymapi.auth.adapter.out.persistence.mapper;

import com.gymapi.auth.adapter.out.persistence.entity.*;
import com.gymapi.auth.domain.model.*;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthPersistenceMapper {

  /**
   * The entity's Lombok getter is {@code isSystem()}, which JavaBeans reads as the property
   * "system", while the record component is "isSystem". Without this mapping the policy above
   * silently drops the flag — and every role read back looks like a non-system role, which disables
   * the guards on updating and deleting seeded roles.
   */
  @Mapping(source = "system", target = "isSystem")
  Role toRole(RoleEntity entity);

  RoleEntity toRoleEntity(Role role);

  Permission toPermission(PermissionEntity entity);

  PermissionEntity toPermissionEntity(Permission permission);

  /** The entity holds a {@code RoleEntity} association where the domain model holds ids. */
  @Mapping(source = "role.id", target = "roleId")
  @Mapping(source = "role.name", target = "roleName")
  UserRole toUserRole(UserRoleEntity entity);

  /**
   * The association cannot be rebuilt from an id alone, so {@code UserRoleRepositoryAdapter}
   * attaches it after mapping. Ignoring it explicitly stops the omission from being invisible.
   */
  @Mapping(target = "role", ignore = true)
  UserRoleEntity toUserRoleEntity(UserRole userRole);

  List<Role> toRoleList(List<RoleEntity> entities);

  List<Permission> toPermissionList(List<PermissionEntity> entities);

  List<UserRole> toUserRoleList(List<UserRoleEntity> entities);
}
