--liquibase formatted sql

--changeset simon:workflow_v1.0.0_20241210_134800
CREATE TABLE t_flow
(
    id              BIGINT PRIMARY KEY,
    version         INTEGER NOT NULL,
    name            VARCHAR(255) NOT NULL,
    remark          VARCHAR(255),
    design_data     CLOB,
    enabled         BOOLEAN NOT NULL,
    tenant_id       BIGINT NOT NULL,
    updated_user    BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    created_at      BIGINT NOT NULL,
    updated_at      BIGINT NOT NULL
);

CREATE INDEX idx_flow_tenant_id ON t_flow (tenant_id);

CREATE TABLE t_flow_history
(
    id               BIGINT PRIMARY KEY,
    flow_id          BIGINT NOT NULL,
    version          INTEGER DEFAULT 1,
    design_data      CLOB,
    user_id          BIGINT NOT NULL,
    created_at       BIGINT NOT NULL,

    CONSTRAINT uk_flow_history_flow_id_version UNIQUE (flow_id, version)
);

CREATE TABLE t_flow_log
(
    id              BIGINT PRIMARY KEY,
    flow_id         BIGINT NOT NULL,
    version         INTEGER NOT NULL,
    start_time      BIGINT NOT NULL,
    time_cost       INTEGER,
    status          VARCHAR(31),
    tenant_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    created_at      BIGINT NOT NULL
);

CREATE INDEX idx_flow_log_tenant_id_flow_id ON t_flow_log (tenant_id, flow_id);

CREATE TABLE t_flow_log_data
(
    id              BIGINT PRIMARY KEY,
    data            CLOB,
    created_at      BIGINT NOT NULL
);

CREATE TABLE t_flow_entity_relation
(
    id                      BIGINT PRIMARY KEY,
    entity_id               BIGINT NOT NULL,
    flow_id                 BIGINT NOT NULL,
    created_at              BIGINT NOT NULL
);

CREATE INDEX idx_flow_entity_relation_entity_id ON t_flow_entity_relation (entity_id);
CREATE INDEX idx_flow_entity_relation_flow_id ON t_flow_entity_relation (flow_id);


-- changeset loong:workflow_v1.1.0_2025022111_162400
ALTER TABLE t_flow
ALTER COLUMN tenant_id VARCHAR(255);

ALTER table t_flow_log
ALTER COLUMN tenant_id VARCHAR(255);

update t_flow set tenant_id = 'default' where tenant_id = '1';
update t_flow_log set tenant_id = 'default' where tenant_id = '1';

-- changeset loong:workflow_v1.1.0_2025040711_162400
ALTER TABLE t_flow
alter column tenant_id set default 'default';

ALTER TABLE t_flow_log
alter column tenant_id set default 'default';