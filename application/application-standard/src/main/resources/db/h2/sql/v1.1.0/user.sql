--liquibase formatted sql

--changeset loong:user_v1.1.0_20241119_155400
alter table `t_user`
    add column tenant_id BIGINT not null default 1;
alter table `t_user`
    add column status VARCHAR(32) not null default 'ENABLE';
CREATE INDEX idx_user_tenant_id ON `t_user` (tenant_id);

CREATE TABLE `t_tenant`
(
    id         BIGINT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    domain     VARCHAR(255) NOT NULL,
    status     VARCHAR(32)  NOT NULL,
    created_at BIGINT       not null,
    updated_at BIGINT
);

CREATE TABLE `t_role`
(
    id          BIGINT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL default 1,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_at  BIGINT       not null,
    updated_at  BIGINT
);
CREATE INDEX idx_role_tenant_id ON `t_role` (tenant_id);

CREATE TABLE `t_user_role`
(
    id         BIGINT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    role_id    BIGINT NOT NULL,
    tenant_id  BIGINT NOT NULL default 1,
    created_at BIGINT not null
);
CREATE INDEX idx_user_role_tenant_id ON `t_user_role` (tenant_id);
CREATE INDEX idx_user_role_user_id ON `t_user_role` (user_id);
CREATE INDEX idx_user_role_role_id ON `t_user_role` (role_id);

CREATE TABLE `t_role_resource`
(
    id            BIGINT PRIMARY KEY,
    role_id       BIGINT       NOT NULL,
    resource_id   VARCHAR(255) NOT NULL,
    resource_type VARCHAR(32)  NOT NULL,
    tenant_id     BIGINT       NOT NULL default 1,
    created_at    BIGINT       not null
);
CREATE INDEX idx_role_resource_tenant_id ON `t_role_resource` (tenant_id);
CREATE INDEX idx_role_resource_role_id ON `t_role_resource` (role_id);

CREATE TABLE `t_menu`
(
    id         BIGINT PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL default 1,
    parent_id  BIGINT,
    code       VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(32)  NOT NULL,
    created_at BIGINT       not null,
    updated_at BIGINT
);
CREATE INDEX idx_menu_tenant_id ON `t_menu` (tenant_id);

CREATE TABLE `t_role_menu`
(
    id         BIGINT PRIMARY KEY,
    role_id    BIGINT NOT NULL,
    menu_id    BIGINT NOT NULL,
    tenant_id  BIGINT NOT NULL default 1,
    created_at BIGINT not null
);
CREATE INDEX idx_role_menu_tenant_id ON `t_role_menu` (tenant_id);
CREATE INDEX idx_role_menu_role_id ON `t_role_menu` (role_id);
CREATE INDEX idx_role_menu_menu_id ON `t_role_menu` (menu_id);

insert into `t_tenant`(id, name, domain, status, created_at, updated_at)
values (1, 'default', 'default', 'ENABLE', 1732005490000, 1732005490000);
insert into `t_role` (id, tenant_id, name, created_at, updated_at)
values (1, 1, 'super_admin', 1732005490000, 1732005490000);

--changeset loong:user_v1.1.0_20241209_092400

insert into `t_menu` (id, parent_id, code, name, type, created_at, updated_at)
VALUES (1000, null, 'dashboard', 'dashboard', 'MENU', 1732005490000, 1732005490000),
       (1001, 1000, 'dashboard.view', 'dashboard.view', 'BUTTON', 1732005490000, 1732005490000),
       (1002, 1000, 'dashboard.add', 'dashboard.add', 'BUTTON', 1732005490000, 1732005490000),
       (1003, 1000, 'dashboard.edit', 'dashboard.edit', 'BUTTON', 1732005490000, 1732005490000),
       (2000, null, 'device', 'device', 'MENU', 1732005490000, 1732005490000),
       (2001, 2000, 'device.view', 'device.view', 'BUTTON', 1732005490000, 1732005490000),
       (2002, 2000, 'device.add', 'device.add', 'BUTTON', 1732005490000, 1732005490000),
       (2003, 2000, 'device.rename', 'device.rename', 'BUTTON', 1732005490000, 1732005490000),
       (2004, 2000, 'device.delete', 'device.delete', 'BUTTON', 1732005490000, 1732005490000),
       (3000, null, 'entity_custom', 'entity_custom', 'MENU', 1732005490000, 1732005490000),
       (3001, 3000, 'entity_custom.view', 'entity_custom.view', 'BUTTON', 1732005490000, 1732005490000),
       (3002, 3000, 'entity_custom.add', 'entity_custom.add', 'BUTTON', 1732005490000, 1732005490000),
       (3003, 3000, 'entity_custom.edit', 'entity_custom.edit', 'BUTTON', 1732005490000, 1732005490000),
       (3004, 3000, 'entity_custom.delete', 'entity_custom.delete', 'BUTTON', 1732005490000, 1732005490000),
       (4000, null, 'entity_data', 'entity_data', 'MENU', 1732005490000, 1732005490000),
       (4001, 4000, 'entity_data.view', 'entity_data.view', 'BUTTON', 1732005490000, 1732005490000),
       (4002, 4000, 'entity_data.edit', 'entity_data.edit', 'BUTTON', 1732005490000, 1732005490000),
       (4003, 4000, 'entity_data.export', 'entity_data.export', 'BUTTON', 1732005490000, 1732005490000),
       (5000, null, 'workflow', 'workflow', 'MENU', 1732005490000, 1732005490000),
       (5001, 5000, 'workflow.view', 'workflow.view', 'BUTTON', 1732005490000, 1732005490000),
       (5002, 5000, 'workflow.add', 'workflow.add', 'BUTTON', 1732005490000, 1732005490000),
       (5003, 5000, 'workflow.import', 'workflow.import', 'BUTTON', 1732005490000, 1732005490000),
       (5004, 5000, 'workflow.edit', 'workflow.edit', 'BUTTON', 1732005490000, 1732005490000),
       (5005, 5000, 'workflow.export', 'workflow.export', 'BUTTON', 1732005490000, 1732005490000),
       (5006, 5000, 'workflow.delete', 'workflow.delete', 'BUTTON', 1732005490000, 1732005490000),
       (5007, 5000, 'workflow.enable', 'workflow.enable', 'BUTTON', 1732005490000, 1732005490000),
       (6000, null, 'integration', 'integration', 'MENU', 1732005490000, 1732005490000),
       (6001, 6000, 'integration.view', 'integration.view', 'BUTTON', 1732005490000, 1732005490000),
       (6002, 6000, 'integration.edit_property', 'integration.edit_property', 'BUTTON', 1732005490000, 1732005490000),
       (6003, 6000, 'integration.edit_service', 'integration.edit_service', 'BUTTON', 1732005490000, 1732005490000)
;


-- changeset loong:user_v1.1.0_20241210_112400
insert into `t_user_role` (id, user_id, role_id, created_at)
SELECT ROW_NUMBER() OVER (ORDER BY t.id), t.id, 1, 1732005490000
FROM `t_user` t;

-- changeset loong:user_v1.1.0_20241211_162400
update `t_menu` set type='FUNCTION' where type='BUTTON';

delete from `t_menu` where code in ('workflow.import','workflow.export','workflow.enable','integration.edit_property','integration.edit_service');

update `t_menu` set code='device.edit',name='device.edit' where code='device.rename';

insert into `t_menu` (id, parent_id, code, name, type, created_at, updated_at)
VALUES (7000, null, 'entity', 'entity', 'MENU', 1732005490000, 1732005490000);

update `t_menu` set parent_id=7000 where id in(3000,4000);

-- changeset loong:user_v1.1.0_2025022111_162400
ALTER TABLE t_tenant
ALTER COLUMN id VARCHAR(255);

ALTER TABLE t_user
ALTER COLUMN tenant_id VARCHAR(255);

ALTER table t_role
ALTER COLUMN tenant_id VARCHAR(255);

ALTER table t_user_role
ALTER COLUMN tenant_id VARCHAR(255);

ALTER table t_role_resource
ALTER COLUMN tenant_id VARCHAR(255);

ALTER table t_menu
ALTER COLUMN tenant_id VARCHAR(255);

ALTER table t_role_menu
ALTER COLUMN tenant_id VARCHAR(255);

update `t_tenant` set id='default' where id='1';
update `t_user` set tenant_id='default' where tenant_id='1';
update `t_role` set tenant_id='default' where tenant_id='1';
update `t_user_role` set tenant_id='default' where tenant_id='1';
update `t_role_resource` set tenant_id='default' where tenant_id='1';
update `t_menu` set tenant_id='default' where tenant_id='1';
update `t_role_menu` set tenant_id='default' where tenant_id='1';

-- changeset loong:user_v1.1.0_2025040711_162400
alter table t_user
alter column tenant_id set default 'default';

alter table t_role
alter column tenant_id set default 'default';

alter table t_user_role
alter column tenant_id set default 'default';

alter table t_role_resource
alter column tenant_id set default 'default';

alter table t_menu
alter column tenant_id set default 'default';

alter table t_role_menu
alter column tenant_id set default 'default';