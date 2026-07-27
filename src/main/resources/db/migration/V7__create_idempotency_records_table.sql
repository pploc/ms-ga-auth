-- Backs the Idempotency-Key header: lets a client safely retry a mutating request after a timeout
-- or a 503 without risking a second role assignment.
--
-- The unique constraint on idempotency_key is what makes concurrent duplicates safe: two racing
-- retries both try to insert, one wins, the loser is told the request is already in flight.
CREATE TABLE idempotency_records (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key       VARCHAR(255) NOT NULL,
    request_method        VARCHAR(10)  NOT NULL,
    request_path          VARCHAR(512) NOT NULL,
    -- SHA-256 of method + path + body: detects a key replayed against a different request.
    request_fingerprint   VARCHAR(64)  NOT NULL,
    state                 VARCHAR(20)  NOT NULL,
    response_status       INTEGER,
    response_content_type VARCHAR(128),
    response_body         TEXT,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_idempotency_records_key UNIQUE (idempotency_key)
);

-- Supports the scheduled purge of records past their retention window.
CREATE INDEX idx_idempotency_records_expires_at ON idempotency_records(expires_at);
