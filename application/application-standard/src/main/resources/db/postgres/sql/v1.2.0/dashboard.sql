--liquibase formatted sql

--changeset loong:dashboard_v1.2.0_20250331_095400
alter table t_dashboard
    add column home boolean default false;

update t_dashboard
set home = true where id = 1;

--changeset loong:dashboard_v1.2.0_20250415_095400
alter table t_dashboard
    drop column home;

create table t_dashboard_home
(
    id         BIGINT PRIMARY KEY,
    tenant_id  VARCHAR(255) not null,
    dashboard_id BIGINT not null,
    user_id    VARCHAR(255) not null,
    created_at BIGINT not null
);

-- changeset loong:dashboard_v1.2.0_20250415_145400
drop table t_dashboard_home;
create table t_dashboard_home
(
    id         BIGINT PRIMARY KEY,
    tenant_id  VARCHAR(255) not null,
    dashboard_id BIGINT not null,
    user_id   BIGINT not null,
    created_at BIGINT not null
);