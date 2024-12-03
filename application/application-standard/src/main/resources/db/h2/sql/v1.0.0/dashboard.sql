--liquibase formatted sql

--changeset loong:dashboard_v1.0.0_20241024_095400
CREATE TABLE `t_dashboard`
(
    id         BIGINT PRIMARY KEY,
    name       VARCHAR(255) not null,
    created_at BIGINT       not null,
    updated_at BIGINT
);
CREATE TABLE `t_dashboard_widget`
(
    id           BIGINT PRIMARY KEY,
    dashboard_id BIGINT not null,
    data         CLOB   not null,
    created_at   BIGINT not null,
    updated_at   BIGINT
);
CREATE INDEX idx_dashboard_widget_dashboard_id ON `t_dashboard_widget` (dashboard_id);

CREATE TABLE `t_dashboard_widget_template`
(
    id         BIGINT PRIMARY KEY,
    name       VARCHAR(255) not null,
    data       CLOB         not null,
    created_at BIGINT       not null,
    updated_at BIGINT
);

INSERT INTO `t_dashboard` (id, name, created_at, updated_at)
VALUES (1, 'IoT Dashboard', 1729845293630, NULL);