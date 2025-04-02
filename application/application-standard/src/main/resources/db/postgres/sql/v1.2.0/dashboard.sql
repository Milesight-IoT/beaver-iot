--liquibase formatted sql

--changeset loong:dashboard_v1.2.0_20250331_095400
alter table t_dashboard
    add column home boolean default false;

update t_dashboard
set home = true where id = 1;