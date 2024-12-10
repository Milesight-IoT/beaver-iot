--liquibase formatted sql

--changeset loong:entity_v1.0.0_20241024_095400
CREATE TABLE `t_entity`
(
    id               BIGINT PRIMARY KEY,
    "key"              VARCHAR(512) not null,
    name             VARCHAR(255) not null,
    type             VARCHAR(255) not null,
    access_mod       VARCHAR(255),
    parent           VARCHAR(512),
    attach_target    VARCHAR(255) not null,
    attach_target_id VARCHAR(255) not null,
    value_attribute  TEXT,
    value_type       VARCHAR(255) not null,
    created_at       BIGINT       not null,
    updated_at       BIGINT,
    UNIQUE ("key")
);
CREATE INDEX idx_entity_attach_target ON `t_entity` (attach_target_id, attach_target);

CREATE TABLE `t_entity_latest`
(
    id            BIGINT PRIMARY KEY,
    entity_id     BIGINT not null,
    value_long    BIGINT,
    value_double  DECIMAL,
    value_boolean BOOLEAN,
    value_string  VARCHAR(1024),
    value_binary  BLOB,
    timestamp     BIGINT not null,
    updated_at    BIGINT
);
CREATE INDEX idx_entity_latest_entity_id ON `t_entity_latest` (entity_id);

CREATE TABLE `t_entity_history`
(
    id            BIGINT PRIMARY KEY,
    entity_id     BIGINT not null,
    value_long    BIGINT,
    value_double  DECIMAL,
    value_boolean BOOLEAN,
    value_string  VARCHAR(1024),
    value_binary  BLOB,
    timestamp     BIGINT not null,
    created_at    BIGINT not null,
    created_by    VARCHAR(255),
    updated_at    BIGINT,
    updated_by    VARCHAR(255),
    UNIQUE (entity_id, timestamp)
);
