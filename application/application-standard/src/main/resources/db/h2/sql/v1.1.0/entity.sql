--liquibase formatted sql

--changeset loong:entity_v1.1.0_20241120_164000
alter table `t_entity`
    add column tenant_id bigint not null default 1;
alter table `t_entity`
    add column user_id bigint;
CREATE INDEX idx_entity_tenant_id ON `t_entity` (tenant_id);

alter table `t_entity_latest`
    add column tenant_id bigint not null default 1;
CREATE INDEX idx_entity_latest_tenant_id ON `t_entity_latest` (tenant_id);

alter table `t_entity_history`
    add column tenant_id bigint not null default 1;
CREATE INDEX idx_entity_history_tenant_id ON `t_entity_history` (tenant_id);

--changeset Maglitch65:entity_v1.1.0_20241129_133000
ALTER TABLE t_entity
    ADD COLUMN visible BOOLEAN DEFAULT TRUE;

--changeset Maglitch65:entity_v1.1.0_20241129_133001
UPDATE t_entity
SET visible = true
WHERE visible IS NULL;

--changeset Maglitch65:entity_v1.1.0_20241129_133002
ALTER TABLE t_entity
    ALTER COLUMN visible SET NOT NULL;

--changeset loong:entity_v1.1.0_20250115_101000
ALTER TABLE t_entity_history
    ALTER COLUMN value_double double;
ALTER TABLE t_entity_latest
    ALTER COLUMN value_double double;

--changeset Simon:entity_v1.1.0_20250208_140100
ALTER TABLE t_entity_history
    ALTER COLUMN value_string VARCHAR(10485760);
ALTER TABLE t_entity_latest
    ALTER COLUMN value_string VARCHAR(10485760);
