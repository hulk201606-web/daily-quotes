-- MySQL 建表脚本（部署到 MySQL 时执行）
CREATE TABLE IF NOT EXISTS quote
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    content           VARCHAR(2000) NOT NULL,
    author            VARCHAR(120)  NOT NULL,
    source            VARCHAR(180)  NOT NULL,
    popularity        INT           NOT NULL,
    featured          TINYINT(1)    NOT NULL DEFAULT 0,
    origin            VARCHAR(20)   NOT NULL,
    fingerprint       VARCHAR(64)   NOT NULL,
    last_fetched_date DATE          NULL,
    original_content  VARCHAR(2000) NULL COMMENT '翻译前原文（外文采集翻译后留存），未翻译为 NULL',
    lang              VARCHAR(10)   NULL COMMENT '内容语种：zh=中文 / en=外文；翻译后 content 为中文、original_content 存原文',
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    CONSTRAINT uk_quote_fingerprint UNIQUE (fingerprint)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_quote_popularity ON quote (popularity DESC);
CREATE INDEX idx_quote_origin ON quote (origin);
CREATE INDEX idx_quote_source ON quote (source);

CREATE TABLE IF NOT EXISTS quote_fetch_log
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    fetch_date     DATE        NOT NULL,
    fetch_type     VARCHAR(20) NOT NULL,
    fetched_count  INT         NOT NULL,
    skipped_count  INT         NOT NULL,
    total_attempts INT         NOT NULL,
    status         VARCHAR(20) NOT NULL,
    error_message  VARCHAR(1000) NULL,
    created_at     DATETIME(6) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_fetch_log_date ON quote_fetch_log (fetch_date);

-- 用户表：支撑「按个人标签/喜好收录」与「角色权限控制」
CREATE TABLE IF NOT EXISTS sys_user
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 用户个人标签字典：同一用户下标签名唯一
CREATE TABLE IF NOT EXISTS user_tag
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    name       VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_user_tag UNIQUE (user_id, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_user_tag_user ON user_tag (user_id);

-- 用户个人收藏（喜好收录）：同一用户对同一名言至多收藏一次
CREATE TABLE IF NOT EXISTS user_favorite
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    quote_id   BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_user_favorite UNIQUE (user_id, quote_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_user_favorite_user ON user_favorite (user_id);
CREATE INDEX idx_user_favorite_quote ON user_favorite (quote_id);

-- 名言-个人标签关联：实现按个人标签归类收录
CREATE TABLE IF NOT EXISTS user_quote_tag
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    quote_id   BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_user_quote_tag UNIQUE (user_id, quote_id, tag_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_user_quote_tag_user ON user_quote_tag (user_id);
CREATE INDEX idx_user_quote_tag_quote ON user_quote_tag (quote_id);
