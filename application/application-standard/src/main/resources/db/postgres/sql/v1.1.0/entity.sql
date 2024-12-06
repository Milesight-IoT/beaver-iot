--liquibase formatted sql

--changeset loong:entity_v1.1.0_20241120_164000
alter table "t_entity"
    add column tenant_id bigint not null default 1, add column user_id bigint;
CREATE INDEX idx_entity_tenant_id ON "t_entity" (tenant_id);

alter table "t_entity_latest"
    add column tenant_id bigint not null default 1;
CREATE INDEX idx_entity_latest_tenant_id ON "t_entity_latest" (tenant_id);

alter table "t_entity_history"
    add column tenant_id bigint not null default 1;
CREATE INDEX idx_entity_history_tenant_id ON "t_entity_history" (tenant_id);