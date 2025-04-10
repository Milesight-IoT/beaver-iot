--liquibase formatted sql

--changeset loong:dashboard_v1.1.0_20241120_152000
alter table "t_dashboard"
    add column tenant_id bigint not null default 1, add column user_id bigint not null default 1;
CREATE INDEX idx_dashboard_tenant_id ON "t_dashboard" (tenant_id);
CREATE INDEX idx_dashboard_user_id ON "t_dashboard" (user_id);

alter table "t_dashboard_widget"
    add column tenant_id bigint not null default 1, add column user_id bigint not null default 1;
CREATE INDEX idx_dashboard_widget_tenant_id ON "t_dashboard_widget" (tenant_id);
CREATE INDEX idx_dashboard_widget_user_id ON "t_dashboard_widget" (user_id);

alter table "t_dashboard_widget_template"
    add column tenant_id bigint not null default 1;
CREATE INDEX idx_dashboard_widget_template_tenant_id ON "t_dashboard_widget_template" (tenant_id);

-- changeset loong:dashboard_v1.1.0_2025022111_162400
alter table t_dashboard
alter column tenant_id type VARCHAR(255);

alter table t_dashboard_widget
alter column tenant_id type VARCHAR(255);

alter table t_dashboard_widget_template
alter column tenant_id type VARCHAR(255);

update t_dashboard set tenant_id = 'default' where tenant_id = '1';
update t_dashboard_widget set tenant_id = 'default' where tenant_id = '1';
update t_dashboard_widget_template set tenant_id = 'default' where tenant_id = '1';

-- changeset loong:dashboard_v1.1.0_2025040711_162400
alter table t_dashboard
alter column tenant_id set default 'default';

alter table t_dashboard_widget
alter column tenant_id set default 'default';

alter table t_dashboard_widget_template
alter column tenant_id set default 'default';