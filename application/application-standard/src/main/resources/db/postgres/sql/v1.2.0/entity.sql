--liquibase formatted sql

--changeset loong:entity_v1.2.0_20250319_155400
alter table t_entity
drop constraint uk_entity_key;

alter table t_entity add constraint uk_entity_key unique(key, tenant_id);

--changeset pandalxb:entity_v1.2.0_20250606_145500
CREATE INDEX idx_entity_parent ON "t_entity" (parent);