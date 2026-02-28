package com.gymapi.auth.adapter.out.persistence.repository;

import com.gymapi.auth.adapter.out.persistence.entity.RoleEntity;
import com.gymapi.auth.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.gymapi.auth.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleRepositoryAdapterTest {

    @Mock
    private RoleJpaRepository roleJpaRepository;

    @Mock
    private AuthPersistenceMapper mapper;

    @InjectMocks
    private RoleRepositoryAdapter adapter;

    @Test
    void save_Success() {
        Role role = new Role(UUID.randomUUID(), "ROLE", "Desc", false, OffsetDateTime.now(), OffsetDateTime.now());
        RoleEntity entity = new RoleEntity();

        when(mapper.toRoleEntity(role)).thenReturn(entity);
        when(roleJpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toRole(entity)).thenReturn(role);

        Role result = adapter.save(role);

        assertNotNull(result);
        verify(roleJpaRepository).save(entity);
    }

    @Test
    void findById_Success() {
        UUID id = UUID.randomUUID();
        RoleEntity entity = new RoleEntity();
        Role role = new Role(id, "ROLE", "Desc", false, OffsetDateTime.now(), OffsetDateTime.now());

        when(roleJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toRole(entity)).thenReturn(role);

        Optional<Role> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().id());
    }

    @Test
    void findByName_Success() {
        RoleEntity entity = new RoleEntity();
        Role role = new Role(UUID.randomUUID(), "ROLE", "Desc", false, OffsetDateTime.now(), OffsetDateTime.now());

        when(roleJpaRepository.findByName("ROLE")).thenReturn(Optional.of(entity));
        when(mapper.toRole(entity)).thenReturn(role);

        Optional<Role> result = adapter.findByName("ROLE");

        assertTrue(result.isPresent());
    }

    @Test
    void existsByName_True() {
        when(roleJpaRepository.existsByName("ROLE")).thenReturn(true);

        assertTrue(adapter.existsByName("ROLE"));
    }

    @Test
    void deleteById_Success() {
        UUID id = UUID.randomUUID();

        adapter.deleteById(id);

        verify(roleJpaRepository).deleteById(id);
    }
}
