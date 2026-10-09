-- Transactional outbox of the audit trail (ADR-031). One table per service schema, in every tenant database.
-- The starter applies this script with its own Flyway history table (audit_flyway_history), so the version
-- number never collides with the migrations of the service.
CREATE TABLE IF NOT EXISTS audit_outbox (
    event_id        uuid         NOT NULL PRIMARY KEY,
    schema_version  varchar(8)   NOT NULL,
    tenant          varchar(63)  NOT NULL,
    source_service  varchar(100) NOT NULL,
    trace_id        varchar(32)  NOT NULL,
    span_id         varchar(16)  NOT NULL,
    payload         text         NOT NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    attempts        integer      NOT NULL DEFAULT 0
);
