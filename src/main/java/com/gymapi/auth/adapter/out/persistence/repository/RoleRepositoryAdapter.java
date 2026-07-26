package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.RoleEntity;
import com.gymapi.auth.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.gymapi.auth.application.port.out.RoleRepository;
import com.gymapi.auth.domain.model.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

  private final RoleJpaRepository roleJpaRepository;
  private final AuthPersistenceMapper mapper;

  @Override
  public Role save(Role role) {
    RoleEntity entity = mapper.toRoleEntity(role);
    RoleEntity saved = roleJpaRepository.save(entity);
    return mapper.toRole(saved);
  }

  @Override
  public Optional<Role> findById(UUID id) {
    return roleJpaRepository.findById(id).map(mapper::toRole);
  }

  @Override
  public Optional<Role> findByName(String name) {
    return roleJpaRepository.findByName(name).map(mapper::toRole);
  }

  @Override
  public List<Role> findAll() {
    return mapper.toRoleList(roleJpaRepository.findAll());
  }

  @Override
  public boolean existsByName(String name) {
    return roleJpaRepository.existsByName(name);
  }

  @Override
  public void deleteById(UUID id) {
    roleJpaRepository.deleteById(id);
  }
}
