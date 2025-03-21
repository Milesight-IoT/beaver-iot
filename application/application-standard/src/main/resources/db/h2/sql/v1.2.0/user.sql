--liquibase formatted sql

--changeset loong:user_v1.2.0_20250224_155400
ALTER TABLE `t_tenant`
    ADD COLUMN time_zone VARCHAR(255) not null DEFAULT 'GMT+08:00';

--changeset loong:user_v1.2.0_20250321_095400
alter table t_user
drop constraint uk_email;

alter table t_user add constraint uk_email unique(email, tenant_id);