--liquibase formatted sql

--changeset pandalxb:blueprint_library_resource_v1.3.0_20250901_092200
CREATE TABLE `t_blueprint_library_resource`
(
    id                      BIGINT        NOT NULL,
    path                    VARCHAR(1024) NOT NULL,
    content                 TEXT          NOT NULL,
    library_id              BIGINT        NOT NULL,
    library_version         VARCHAR(32)   NOT NULL,
    created_at              BIGINT        NOT NULL,
    updated_at              BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blueprint_library_resource_library_id_version_path UNIQUE (library_id, library_version, path)
);