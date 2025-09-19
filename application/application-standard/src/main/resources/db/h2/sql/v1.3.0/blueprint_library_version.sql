--liquibase formatted sql

--changeset pandalxb:blueprint_library_version_v1.3.0_20250919_100000
CREATE TABLE `t_blueprint_library_version`
(
    id                      BIGINT        NOT NULL,
    library_id              BIGINT        NOT NULL,
    library_version         VARCHAR(32)   NOT NULL,
    synced_at               BIGINT        NOT NULL,
    created_at              BIGINT        NOT NULL,
    updated_at              BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blueprint_library_version_library_id_library_version UNIQUE (library_id, library_version)
);