--liquibase formatted sql

--changeset simon:user_v1.3.0_20250604_092400
insert into "t_menu" (id, parent_id, code, name, type, created_at, updated_at)
VALUES (2005, 2000, 'device.group_manage', 'device.group_manage', 'FUNCTION', 1751592395895, 1751592395895);;

--changeset Maglitch65:user_v1.3.0_20250708_130000
insert into "t_menu" (id, parent_id, code, name, type, created_at, updated_at)
VALUES (9000, null, 'tag', 'tag', 'MENU', 1751950800000, 1751950800000),
       (9001, 9000, 'entity_tag.view', 'entity_tag.view', 'FUNCTION', 1751950800000, 1751950800000),
       (9002, 9000, 'entity_tag.edit', 'entity_tag.edit', 'FUNCTION', 1751950800000, 1751950800000);
