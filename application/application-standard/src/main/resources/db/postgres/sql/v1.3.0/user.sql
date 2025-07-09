--liquibase formatted sql

--changeset simon:user_v1.3.0_20250604_092400
insert into "t_menu" (id, parent_id, code, name, type, created_at, updated_at)
VALUES (2005, 2000, 'device.group_manage', 'device.group_manage', 'FUNCTION', 1751592395895, 1751592395895);;
