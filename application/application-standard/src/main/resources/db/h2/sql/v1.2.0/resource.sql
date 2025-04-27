--liquibase formatted sql

--changeset simon:resource_v1.2.0_20250407_170000
CREATE TABLE `t_resource_data`
(
    id                      BIGINT NOT NULL,
    obj_key                 VARCHAR(512) NOT NULL,
    content_type            VARCHAR(255) NOT NULL,
    content_length          BIGINT NOT NULL,
    data                    BLOB,
    created_at              BIGINT NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (obj_key)
);

CREATE TABLE `t_resource`
(
    id                      BIGINT NOT NULL,
    "key"                   VARCHAR(512) NOT NULL,
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

    PRIMARY KEY (id),
    UNIQUE ("key"),
    UNIQUE (url)
);

CREATE TABLE `t_resource_temp`
(
    id                      BIGINT NOT NULL,
    resource_id             BIGINT NOT NULL,
    created_at              BIGINT NOT NULL,
    expired_at              BIGINT NOT NULL,

    PRIMARY KEY (id)
);

CREATE INDEX idx_resource_temp_expired_at ON `t_resource_temp` (expired_at);
CREATE INDEX idx_resource_temp_resource_id ON `t_resource_temp` (resource_id);

CREATE TABLE `t_resource_ref`
(
    id                      BIGINT NOT NULL,
    ref_id                  VARCHAR(255) NOT NULL,
    ref_type                VARCHAR(255) NOT NULL,
    resource_id             BIGINT NOT NULL,
    created_at              BIGINT NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (ref_type, ref_id, resource_id)
);

CREATE INDEX idx_resource_ref_resource_id ON `t_resource_ref` (resource_id);


--changeset simon:resource_v1.2.0_20250427_101200
ALTER TABLE `t_resource_temp`
    ADD COLUMN settled BOOLEAN not null DEFAULT false;