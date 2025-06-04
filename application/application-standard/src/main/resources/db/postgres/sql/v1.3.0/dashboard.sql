--liquibase formatted sql

--changeset Maglitch65:dashboard_v1.3.0_20250528_100000
CREATE TABLE "t_dashboard_entity"
(
    id           BIGINT PRIMARY KEY,
    dashboard_id BIGINT       not null,
    entity_id    BIGINT       not null,
    entity_key   VARCHAR(512) not null,
    created_at   BIGINT       not null,
    updated_at   BIGINT,
    CONSTRAINT uk_dashboard_entity UNIQUE (dashboard_id, entity_id)
);
