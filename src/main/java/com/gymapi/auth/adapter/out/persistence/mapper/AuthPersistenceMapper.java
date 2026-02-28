package com.gymapi.auth.adapter.out.persistence.mapper;

import com.gymapi.auth.adapter.out.persistence.entity.*;
import com.gymapi.auth.domain.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthPersistenceMapper {

    Role toRole(RoleEntity entity);

    RoleEntity toRoleEntity(Role role);

    Permission toPermission(PermissionEntity entity);

    PermissionEntity toPermissionEntity(Permission permission);

    UserRole toUserRole(UserRoleEntity entity);

    UserRoleEntity toUserRoleEntity(UserRole userRole);

    List<Role> toRoleList(List<RoleEntity> entities);
    List<Permission> toPermissionList(List<PermissionEntity> entities);
    List<UserRole> toUserRoleList(List<UserRoleEntity> entities);
}
