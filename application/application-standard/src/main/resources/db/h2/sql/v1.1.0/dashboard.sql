--liquibase formatted sql

--changeset loong:dashboard_v1.1.0_20241120_152000
alter table `t_dashboard`
    add column tenant_id bigint not null default 1;
alter table `t_dashboard`
    add column user_id bigint not null default 1;
CREATE INDEX idx_dashboard_tenant_id ON `t_dashboard` (tenant_id);
CREATE INDEX idx_dashboard_user_id ON `t_dashboard` (user_id);

alter table `t_dashboard_widget`
    add column tenant_id bigint not null default 1;
alter table `t_dashboard_widget`
    add column user_id bigint not null default 1;
CREATE INDEX idx_dashboard_widget_tenant_id ON `t_dashboard_widget` (tenant_id);
CREATE INDEX idx_dashboard_widget_user_id ON `t_dashboard_widget` (user_id);

alter table `t_dashboard_widget_template`
    add column tenant_id bigint not null default 1;
CREATE INDEX idx_dashboard_widget_template_tenant_id ON `t_dashboard_widget_template` (tenant_id);