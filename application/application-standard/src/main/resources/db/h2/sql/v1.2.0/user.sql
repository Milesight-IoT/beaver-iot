--liquibase formatted sql

--changeset loong:user_v1.2.0_20250224_155400
ALTER TABLE `t_tenant`
    ADD COLUMN time_zone VARCHAR(255) not null DEFAULT 'GMT+08:00';

--changeset loong:user_v1.2.0_20250321_095400
alter table t_user
drop constraint uk_email;

alter table t_user add constraint uk_email unique(email, tenant_id);


--changeset Maglitch65:user_v1.2.0_20250407_153000
insert into `t_menu` (id, parent_id, code, name, type, created_at, updated_at)
VALUES (8000, null, 'credentials', 'credentials', 'MENU', 1744010508000, 1744010508000),
       (8001, 8000, 'credentials.view', 'credentials.view', 'FUNCTION', 1744010508000, 1744010508000),
       (8002, 8000, 'credentials.edit', 'credentials.edit', 'FUNCTION', 1744010508000, 1744010508000);
