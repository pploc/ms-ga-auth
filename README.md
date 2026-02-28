# ms-ga-auth - Authorization Management Service

## Overview

| Property         | Value                                     |
| ---------------- | ----------------------------------------- |
| **Language**     | Java 21                                   |
| **Framework**    | Spring Boot 3.2.3                         |
| **Database**     | PostgreSQL 15 (Spring Data JPA + Flyway) |
| **Port**         | 8082                                      |
| **Base Path**    | `/auth`                                   |
| **Architecture** | Hexagonal Architecture (Ports & Adapters) |

**Purpose:** The Authorization Service manages RBAC - defining roles, permissions, and their mappings, and assigning roles to users.

---

## Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose

### Run with Docker Compose

```bash
docker-compose up -d
```

### Run Locally

```bash
./gradlew bootRun
```

---

## API Endpoints

### Role Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/roles` | Get all roles |
| POST | `/auth/roles` | Create role |
| GET | `/auth/roles/{id}` | Get role by ID |
| PUT | `/auth/roles/{id}` | Update role |
| DELETE | `/auth/roles/{id}` | Delete role |
| GET | `/auth/roles/{id}/permissions` | Get role permissions |
| PUT | `/auth/roles/{id}/permissions` | Set role permissions |

### Permission Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/permissions` | Get all permissions |
| POST | `/auth/permissions` | Create permission |
| GET | `/auth/permissions/{id}` | Get permission by ID |
| PUT | `/auth/permissions/{id}` | Update permission |
| DELETE | `/auth/permissions/{id}` | Delete permission |

### User Role Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/users/{userId}/roles` | Get user's roles |
| POST | `/auth/users/{userId}/roles` | Assign role to user |
| DELETE | `/auth/users/{userId}/roles/{roleId}` | Revoke role |
| GET | `/auth/users/{userId}/roles/with-permissions` | Get roles with permissions |

---

## Default Roles

- **SUPER_ADMIN** - Full system access (system role)
- **GYM_ADMIN** - Gym management (system role)
- **TRAINER** - Trainer permissions (system role)
- **MEMBER** - Self-service (system role)
- **STAFF** - Front desk operations (system role)

---

## OpenAPI Documentation

After running the application, visit: `http://localhost:8082/swagger-ui.html`

---

## Kafka Events

- `auth.role_assigned` - When a role is assigned to a user
- `auth.role_revoked` - When a role is revoked from a user
- `auth.permission_changed` - When permissions are updated

---

## Technology Stack

- Spring Boot 3.2.3
- Spring Data JPA
- Spring Security + JWT
- Spring Kafka
- PostgreSQL 15
- Flyway
- MapStruct
- Lombok
- OpenAPI 3.0
