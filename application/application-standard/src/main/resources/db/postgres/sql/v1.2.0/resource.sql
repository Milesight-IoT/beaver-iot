--liquibase formatted sql

--changeset simon:resource_v1.2.0_20250407_170000
CREATE TABLE "t_resource_data"
(
    id                      BIGINT PRIMARY KEY,
    obj_key                 VARCHAR(512) NOT NULL,
    content_type            VARCHAR(255) NOT NULL,
    content_length          BIGINT NOT NULL,
    data                    BYTEA,
    created_at              BIGINT NOT NULL,
    CONSTRAINT uk_obj_key UNIQUE (obj_key)
);

CREATE TABLE "t_resource"
(
    id                      BIGINT PRIMARY KEY,
    key                     VARCHAR(512) NOT NULL,
    url                     VARCHAR(512) NOT NULL,
    tenant_id               VARCHAR(255) NOT NULL,
    name                    VARCHAR(512) NOT NULL,
    content_type            VARCHAR(255),
    content_length          BIGINT,
    description             VARCHAR(512),
    created_at              BIGINT       NOT NULL,
    created_by              VARCHAR(255)     DEFAULT NULL,
    updated_at              BIGINT       NOT NULL,
    updated_by              VARCHAR(255)     DEFAULT NULL,

    CONSTRAINT uk_resource_key UNIQUE (key),
    CONSTRAINT uk_resource_url UNIQUE (url)
);

CREATE TABLE "t_resource_temp"
(
    id                      BIGINT PRIMARY KEY,
    resource_id             BIGINT NOT NULL,
    created_at              BIGINT NOT NULL,
    expired_at              BIGINT NOT NULL
);

CREATE INDEX idx_resource_temp_expired_at ON "t_resource_temp" (expired_at);
CREATE INDEX idx_resource_temp_resource_id ON "t_resource_temp" (resource_id);

CREATE TABLE "t_resource_ref"
(
    id                      BIGINT PRIMARY KEY,
    ref_id                  VARCHAR(255) NOT NULL,
    ref_type                VARCHAR(255) NOT NULL,
    resource_id             BIGINT NOT NULL,
    created_at              BIGINT NOT NULL,

    CONSTRAINT uk_resource_ref_ref_type_ref_id_resource_id UNIQUE (ref_type, ref_id, resource_id)
);

CREATE INDEX idx_resource_ref_resource_id ON "t_resource_ref" (resource_id);

--changeset simon:resource_v1.2.0_20250427_101200
alter table t_resource_temp
    add column settled boolean not null default false;