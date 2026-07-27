package com.gymapi.auth.application.port.out;

import com.gymapi.auth.domain.model.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
  Role save(Role role);

  Optional<Role> findById(UUID id);

  Optional<Role> findByName(String name);

  List<Role> findAll();

  boolean existsByName(String name);

  void deleteById(UUID id);
}
