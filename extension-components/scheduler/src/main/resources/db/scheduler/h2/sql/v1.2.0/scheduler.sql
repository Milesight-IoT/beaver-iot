--liquibase formatted sql

--changeset Maglitch65:scheduler_v1.2.0_20250421_093000
CREATE TABLE `t_scheduled_task`
(
    id                     BIGINT PRIMARY KEY,
    task_key               VARCHAR(512) not null,
    execution_epoch_second BIGINT       not null,
    triggered_at           BIGINT       not null DEFAULT 0,
    attempts               INTEGER      not null DEFAULT 0,
    iteration              INTEGER      not null DEFAULT 0,
    created_at             BIGINT       not null,
    updated_at             BIGINT,
    UNIQUE (task_key, execution_epoch_second, triggered_at)
);
CREATE INDEX idx_time ON `t_scheduled_task` (triggered_at, execution_epoch_second);


--changeset Maglitch65:scheduler_v1.2.0_20250421_093001
CREATE TABLE `t_schedule_settings`
(
    id                     BIGINT PRIMARY KEY,
    task_key               VARCHAR(512) not null,
    schedule_type          VARCHAR(255) not null,
    schedule_rule          TEXT not null,
    payload                TEXT,
    tenant_id              VARCHAR(255),
    created_at             BIGINT       not null,
    updated_at             BIGINT,
    UNIQUE (task_key)
);
