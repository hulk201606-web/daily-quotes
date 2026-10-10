-- PostgreSQL 建表脚本（部署到 PostgreSQL 时执行）
CREATE TABLE IF NOT EXISTS quote
(
    id                BIGSERIAL PRIMARY KEY,
    content           VARCHAR(2000) NOT NULL,
    author            VARCHAR(120)  NOT NULL,
    source            VARCHAR(180)  NOT NULL,
    popularity        INT           NOT NULL,
    featured          SMALLINT      NOT NULL DEFAULT 0,
    origin            VARCHAR(20)   NOT NULL,
    fingerprint       VARCHAR(64)   NOT NULL,
    last_fetched_date DATE          NULL,
    created_at        TIMESTAMP(6)  NOT NULL,
    updated_at        TIMESTAMP(6)  NOT NULL,
    CONSTRAINT uk_quote_fingerprint UNIQUE (fingerprint)
);

CREATE INDEX idx_quote_popularity ON quote (popularity DESC);
CREATE INDEX idx_quote_origin ON quote (origin);
CREATE INDEX idx_quote_source ON quote (source);

CREATE TABLE IF NOT EXISTS quote_fetch_log
(
    id             BIGSERIAL PRIMARY KEY,
    fetch_date     DATE        NOT NULL,
    fetch_type     VARCHAR(20) NOT NULL,
    fetched_count  INT         NOT NULL,
    skipped_count  INT         NOT NULL,
    total_attempts INT         NOT NULL,
    status         VARCHAR(20) NOT NULL,
    error_message  VARCHAR(1000) NULL,
    created_at     TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_fetch_log_date ON quote_fetch_log (fetch_date);
