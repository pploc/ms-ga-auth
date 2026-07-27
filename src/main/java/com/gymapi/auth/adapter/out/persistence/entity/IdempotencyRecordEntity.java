package com.gymapi.auth.adapter.out.persistence.entity;

import com.gymapi.auth.domain.model.IdempotencyState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
    name = "idempotency_records",
    uniqueConstraints =
        @UniqueConstraint(name = "uq_idempotency_records_key", columnNames = "idempotency_key"),
    indexes = @Index(name = "idx_idempotency_records_expires_at", columnList = "expires_at"))
@Getter
@Setter
@ToString(exclude = "responseBody")
@EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecordEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
  private String idempotencyKey;

  @Column(name = "request_method", nullable = false, length = 10)
  private String requestMethod;

  @Column(name = "request_path", nullable = false, length = 512)
  private String requestPath;

  @Column(name = "request_fingerprint", nullable = false, length = 64)
  private String requestFingerprint;

  @Enumerated(EnumType.STRING)
  @Column(name = "state", nullable = false, length = 20)
  private IdempotencyState state;

  @Column(name = "response_status")
  private Integer responseStatus;

  @Column(name = "response_content_type", length = 128)
  private String responseContentType;

  // columnDefinition rather than @Lob: on PostgreSQL, Hibernate treats a @Lob String as a large
  // object, so `ddl-auto: validate` expects an `oid` column and rejects the `text` the migration
  // creates. This matches how the other TEXT columns in this schema are mapped.
  @Column(name = "response_body", columnDefinition = "TEXT")
  private String responseBody;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;
}
