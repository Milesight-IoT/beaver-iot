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
    ALTER COLUMN value_double set data type double precision;
ALTER TABLE t_entity_latest
    ALTER COLUMN value_double set data type double precision;

--changeset Simon:entity_v1.1.0_20250208_140100
ALTER TABLE t_entity_history
    ALTER COLUMN value_string TYPE VARCHAR(10485760);
ALTER TABLE t_entity_latest
    ALTER COLUMN value_string TYPE VARCHAR(10485760);

-- changeset loong:entity_v1.1.0_20250211_112200
ALTER table t_entity
    add column description TEXT;


-- changeset loong:entity_v1.1.0_2025022111_162400
alter table t_entity
alter column tenant_id type varchar(255);

alter table t_entity_history
alter column tenant_id type varchar(255);

alter table t_entity_latest
alter column tenant_id type varchar(255);

update t_entity set tenant_id = 'default' where tenant_id = '1';
update t_entity_history set tenant_id = 'default' where tenant_id = '1';
update t_entity_latest set tenant_id = 'default' where tenant_id = '1';

-- changeset loong:entity_v1.1.0_20250304_100000
alter table t_entity_history
    drop constraint uk_entity_history;

-- changeset loong:entity_v1.1.0_20250407_165500
alter table t_entity
alter column tenant_id set default 'default';

alter table t_entity_history
alter column tenant_id set default 'default';

alter table t_entity_latest
alter column tenant_id set default 'default';