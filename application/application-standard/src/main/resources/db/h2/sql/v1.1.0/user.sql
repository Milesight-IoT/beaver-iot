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