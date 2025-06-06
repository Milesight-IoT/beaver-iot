--liquibase formatted sql

--changeset loong:entity_v1.2.0_20250319_155400
EXECUTE IMMEDIATE 'ALTER TABLE t_entity DROP CONSTRAINT ' ||
                  (SELECT CONSTRAINT_NAME
                   FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                   WHERE TABLE_NAME = 'T_ENTITY' AND CONSTRAINT_TYPE = 'UNIQUE'
    LIMIT 1);

alter table t_entity add constraint uk_entity_key unique("key", tenant_id);

--changeset pandalxb:entity_v1.2.0_20250606_145500
CREATE INDEX idx_entity_parent ON `t_entity` (parent);