--liquibase formatted sql

--changeset loong:device_v1.1.0_20241122_164400
alter table `t_device`
    add column tenant_id bigint not null default 1;
alter table `t_device`
    add column user_id bigint;
CREATE INDEX idx_device_tenant_id ON `t_device` (tenant_id);


-- changeset loong:device_v1.1.0_2025022111_162400
alter table t_device
alter column tenant_id varchar(255);

update t_device set tenant_id = 'default' where tenant_id = '1';

-- changeset loong:device_v1.1.0_2025040711_162400
alter table t_device
alter column tenant_id set default 'default';