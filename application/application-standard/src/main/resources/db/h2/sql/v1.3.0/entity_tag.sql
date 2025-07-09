--liquibase formatted sql

--changeset Maglitch65:entity_tag_v1.3.0_20250708_130000
CREATE TABLE `t_entity_tag`
(
    id          BIGINT PRIMARY KEY,
    tenant_id   VARCHAR(255) not null,
    name        VARCHAR(255) not null,
    description VARCHAR(255) default null,
    color       VARCHAR(255) not null,
    created_at  BIGINT       not null,
    updated_at  BIGINT,
    UNIQUE (tenant_id, name)
);

--changeset Maglitch65:entity_tag_v1.3.0_20250708_130001
CREATE TABLE `t_entity_tag_mapping`
(
    id         BIGINT PRIMARY KEY,
    entity_id  BIGINT       not null,
    tag_id     BIGINT       not null,
    tenant_id  VARCHAR(255) not null,
    created_at BIGINT       not null,
    updated_at BIGINT,
    UNIQUE (entity_id, tag_id)
);
CREATE INDEX idx_tag_id ON `t_entity_tag_mapping` (tag_id);
