package com.gymapi.auth.adapter.out.persistence.mapper;

import com.gymapi.auth.adapter.out.persistence.entity.*;
import com.gymapi.auth.domain.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthPersistenceMapper {

    Role toRole(RoleEntity entity);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "permission", ignore = true)
    RoleEntity toRoleEntity(Role role);

    Permission toPermission(PermissionEntity entity);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "permission", ignore = true)
    PermissionEntity toPermissionEntity(Permission permission);

    UserRole toUserRole(UserRoleEntity entity);

    @Mapping(target = "role", ignore = true)
    UserRoleEntity toUserRoleEntity(UserRole userRole);

    List<Role> toRoleList(List<RoleEntity> entities);
    List<Permission> toPermissionList(List<PermissionEntity> entities);
    List<UserRole> toUserRoleList(List<UserRoleEntity> entities);
}
