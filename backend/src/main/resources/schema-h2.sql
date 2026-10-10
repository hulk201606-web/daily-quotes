-- H2 初始化脚本（仅 h2 profile 自动执行）
DROP TABLE IF EXISTS quote_fetch_log;
DROP TABLE IF EXISTS quote;

CREATE TABLE quote
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    content          VARCHAR(2000) NOT NULL,
    author           VARCHAR(120)  NOT NULL,
    source           VARCHAR(180)  NOT NULL,
    popularity       INT           NOT NULL,
    featured         BIT           NOT NULL DEFAULT 0,
    origin           VARCHAR(20)   NOT NULL,
    fingerprint      VARCHAR(64)   NOT NULL,
    last_fetched_date DATE         NULL,
    original_content VARCHAR(2000) NULL,
    lang             VARCHAR(10)   NULL,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    CONSTRAINT uk_quote_fingerprint UNIQUE (fingerprint)
);

CREATE INDEX idx_quote_popularity ON quote (popularity DESC);
CREATE INDEX idx_quote_origin ON quote (origin);
CREATE INDEX idx_quote_source ON quote (source);

CREATE TABLE quote_fetch_log
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    fetch_date    DATE        NOT NULL,
    fetch_type    VARCHAR(20) NOT NULL,
    fetched_count INT         NOT NULL,
    skipped_count INT         NOT NULL,
    total_attempts INT        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000) NULL,
    created_at    TIMESTAMP   NOT NULL
);

CREATE INDEX idx_fetch_log_date ON quote_fetch_log (fetch_date);

-- 用户、个人标签、收藏、名言打标（H2 内嵌库：随主表一并重置，便于本地开发）
DROP TABLE IF EXISTS user_quote_tag;
DROP TABLE IF EXISTS user_favorite;
DROP TABLE IF EXISTS user_tag;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE TABLE user_tag
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    name       VARCHAR(64) NOT NULL,
    created_at TIMESTAMP  NOT NULL,
    CONSTRAINT uk_user_tag UNIQUE (user_id, name)
);

CREATE INDEX idx_user_tag_user ON user_tag (user_id);

CREATE TABLE user_favorite
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    quote_id   BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_favorite UNIQUE (user_id, quote_id)
);

CREATE INDEX idx_user_favorite_user ON user_favorite (user_id);
CREATE INDEX idx_user_favorite_quote ON user_favorite (quote_id);

CREATE TABLE user_quote_tag
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    quote_id   BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_quote_tag UNIQUE (user_id, quote_id, tag_id)
);

CREATE INDEX idx_user_quote_tag_user ON user_quote_tag (user_id);
CREATE INDEX idx_user_quote_tag_quote ON user_quote_tag (quote_id);
