package com.gymapi.auth.adapter.in.web.mapper;

import com.gymapi.auth.adapter.in.web.dto.generated.*;
import com.gymapi.auth.domain.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthWebMapper {

    Role toRole(CreateRoleRequest request);

    Role toRole(UpdateRoleRequest request);

    RoleResponse toRoleResponse(Role role);

    Permission toPermission(CreatePermissionRequest request);

    Permission toPermission(UpdatePermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);

    UserRole toUserRole(AssignRoleRequest request);

    UserRoleResponse toUserRoleResponse(UserRole userRole);

    List<RoleResponse> toRoleResponseList(List<Role> roles);

    List<PermissionResponse> toPermissionResponseList(List<Permission> permissions);

    List<UserRoleResponse> toUserRoleResponseList(List<UserRole> userRoles);
}
