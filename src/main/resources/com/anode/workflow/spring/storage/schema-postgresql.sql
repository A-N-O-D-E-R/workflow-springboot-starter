-- Durable storage of the workflow engine (JDBC storage type).
-- One JSON document per persistence key (journey_<case>, worflow_info_<case>, journey_sla_<case>,
-- audit_log_<case>_<seq>...) and the engine's counters.
CREATE TABLE IF NOT EXISTS workflow_document (
    doc_key    VARCHAR(512) PRIMARY KEY,
    kind       VARCHAR(16)  NOT NULL,
    content    TEXT         NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS workflow_counter (
    counter_key VARCHAR(512) PRIMARY KEY,
    value       BIGINT       NOT NULL
);
