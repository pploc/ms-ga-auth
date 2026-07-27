package com.gymapi.auth.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs every Flyway migration against a real Postgres 15. The H2 suites cannot do this — the
 * migrations are Postgres-specific ({@code gen_random_uuid}, {@code TIMESTAMPTZ}, {@code DO}
 * blocks), which is exactly why they were untested before this class existed.
 *
 * <p>Skips itself when no Docker daemon is available, so the build stays runnable on machines
 * without one; CI, which has Docker, always exercises it.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Flyway migrations against real Postgres")
class FlywayMigrationsTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:15-alpine");

  private static Connection connection;

  @BeforeAll
  static void migrateAndConnect() throws SQLException {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
    connection =
        DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  @AfterAll
  static void disconnect() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  void everyTableTheServiceMapsExists() throws SQLException {
    // migrate() in @BeforeAll is the real assertion — a broken migration fails there. This spot
    // check catches a migration that succeeds without creating what the entities map.
    for (String table :
        new String[] {
          "roles", "permissions", "role_permissions", "user_roles", "idempotency_records"
        }) {
      assertThat(
              count("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", table))
          .as("table %s", table)
          .isEqualTo(1);
    }
  }

  @Test
  void seedsTheFiveSystemRoles() throws SQLException {
    assertThat(count("SELECT COUNT(*) FROM roles WHERE is_system = TRUE")).isEqualTo(5);
    assertThat(count("SELECT COUNT(*) FROM roles")).isEqualTo(5);
  }

  @Test
  void superAdminHoldsEveryPermission() throws SQLException {
    long permissions = count("SELECT COUNT(*) FROM permissions");
    assertThat(grantCount("SUPER_ADMIN")).isEqualTo(permissions);
  }

  /** The V8 seed: the matrix in security.md §5, so no role starts with an empty permission set. */
  @Test
  void seedsTheMatrixForTheOtherFourRoles() throws SQLException {
    assertThat(grantCount("GYM_ADMIN")).isEqualTo(12);
    assertThat(grantCount("TRAINER")).isEqualTo(8);
    assertThat(grantCount("MEMBER")).isEqualTo(17);
    assertThat(grantCount("STAFF")).isEqualTo(7);
  }

  @Test
  void gymAdminReadsRolesButDoesNotManageThem() throws SQLException {
    assertThat(holds("GYM_ADMIN", "role", "read")).isTrue();
    assertThat(holds("GYM_ADMIN", "role", "manage")).isFalse();
    assertThat(holds("SUPER_ADMIN", "role", "manage")).isTrue();
  }

  @Test
  void membersManageOnlyWhatIsTheirOwn() throws SQLException {
    assertThat(holds("MEMBER", "booking", "cancel_own")).isTrue();
    assertThat(holds("MEMBER", "booking", "manage")).isFalse();
    assertThat(holds("STAFF", "payment", "manage")).isFalse();
  }

  private static long grantCount(String roleName) throws SQLException {
    return count(
        "SELECT COUNT(*) FROM role_permissions rp JOIN roles r ON r.id = rp.role_id"
            + " WHERE r.name = ?",
        roleName);
  }

  private static boolean holds(String roleName, String resource, String action)
      throws SQLException {
    return count(
            "SELECT COUNT(*) FROM role_permissions rp"
                + " JOIN roles r ON r.id = rp.role_id"
                + " JOIN permissions p ON p.id = rp.permission_id"
                + " WHERE r.name = ? AND p.resource = ? AND p.action = ?",
            roleName,
            resource,
            action)
        == 1;
  }

  private static long count(String sql, String... parameters) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setString(i + 1, parameters[i]);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong(1);
      }
    }
  }
}
