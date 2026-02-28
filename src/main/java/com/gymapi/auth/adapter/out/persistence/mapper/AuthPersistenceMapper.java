package com.gymapi.auth.adapter.out.persistence.mapper;

import com.gymapi.auth.adapter.out.persistence.entity.PermissionEntity;
import com.gymapi.auth.adapter.out.persistence.entity.RoleEntity;
import com.gymapi.auth.adapter.out.persistence.entity.UserRoleEntity;
import com.gymapi.auth.domain.model.Permission;
import com.gymapi.auth.domain.model.Role;
import com.gymapi.auth.domain.model.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthPersistenceMapper {

    Role mapToDomain(RoleEntity entity);

    RoleEntity mapToEntity(Role domain);

    Permission mapToDomain(PermissionEntity entity);

    PermissionEntity mapToEntity(Permission domain);

    @Mapping(source = "role.id", target = "roleId")
    UserRole mapToDomain(UserRoleEntity entity);

    @Mapping(source = "roleId", target = "role.id")
    UserRoleEntity mapToEntity(UserRole domain);
}
