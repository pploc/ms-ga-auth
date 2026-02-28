# ms-ga-auth Implementation Plan

## Overview
This document outlines the step-by-step implementation plan for the `ms-ga-auth` microservice based on the specifications in `ms-ga-auth.md`. The service will be built using Java 21, Spring Boot 3.x, PostgreSQL 15, and a Hexagonal Architecture (Ports & Adapters).

## Phase 1: Project Setup and Infrastructure (Day 1)
1.  **Initialize Project:**
    *   Create a new Spring Boot project (Java 21, Maven/Gradle).
    *   Add required dependencies: Web, Data JPA, Validation, PostgreSQL driver, Flyway, MapStruct, Lombok, and Spring Kafka.
    *   Set up project structure following Hexagonal Architecture: `adapter/in/web`, `adapter/out/persistence`, `adapter/out/messaging`, `application/port/in`, `application/port/out`, `application/service`, `domain/model`, and `config`.
2.  **Database & Migrations Configuration:**
    *   Create `docker-compose.yml` with PostgreSQL (port 5434 mapped to 5432) and Flyway services.
    *   Configure `application.yml` and `application-dev.yml` for database connection and Flyway.
    *   Write Flyway migration scripts (`V1` to `V5`) establishing `roles`, `permissions`, `role_permissions`, and `user_roles` tables along with seed data.
3.  **Basic Configurations:**
    *   Set up `WebConfig` and `SecurityConfig` (JWT parsing/validation).
    *   Configure global exception handling for standardizing API error responses.

## Phase 2: Domain Modeling and Persistence Adapters (Day 2)
1.  **Domain Models:**
    *   Define core domain models: `Role`, `Permission`, `UserRole`, `RolesWithPermissions`.
2.  **Persistence Entities:**
    *   Create JPA entities (`RoleEntity`, `PermissionEntity`, `RolePermissionEntity`, `UserRoleEntity`) mapping to the database tables.
3.  **Repositories and Mappers:**
    *   Create Spring Data JPA repositories (`RoleJpaRepository`, etc.).
    *   Implement `AuthPersistenceMapper` using MapStruct to map between Domain Models and JPA Entities.
    *   Implement outgoing adapter classes (e.g., `RoleRepositoryAdapter`) that implement the `application/port/out` interfaces and use the JPA repositories.

## Phase 3: Application Services and Business Logic (Day 3)
1.  **Port Definitions:**
    *   Define input ports (Use Cases): `RoleUseCase`, `PermissionUseCase`, `UserRoleUseCase`.
    *   Define output ports: `RoleRepository`, `PermissionRepository`, `EventPublisher`.
2.  **Service Implementations:**
    *   Implement `RoleService`, `PermissionService`, and `UserRoleService` executing the business logic.
    *   Ensure validation rules are applied (e.g., preventing deletion of system roles, preventing duplicate role names).

## Phase 4: Web Adapters (REST API) (Day 4)
1.  **DTOs and Web Mapper:**
    *   Create Request and Response DTOs matching the API specifications.
    *   Implement `AuthWebMapper` via MapStruct to map between DTOs and Domain Models.
2.  **REST Controllers:**
    *   Implement `RoleController` (`/auth/roles`).
    *   Implement `PermissionController` (`/auth/permissions`).
    *   Implement `UserRoleController` (`/auth/users/:userId/roles*`).
    *   Wire controllers to Use Cases.

## Phase 5: Messaging Adapter and Finalization (Day 5)
1.  **Kafka Integration:**
    *   Configure `KafkaConfig` in `application.yml`.
    *   Implement `KafkaEventPublisher` mapped to the `EventPublisher` output port to publish `auth.role_assigned`, `auth.role_revoked`, and `auth.permission_changed` events.
2.  **Testing:**
    *   Write unit tests for domain logic and services.
    *   Write integration tests for web and persistence layers.
3.  **Documentation & Polish:**
    *   Add Swagger/OpenAPI annotations to controllers.
    *   Finalize `README.md`.
