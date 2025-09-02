--liquibase formatted sql

--changeset pandalxb:blueprint_repository_resource_v1.3.0_20250901_092200
CREATE TABLE `t_blueprint_repository_resource`
(
    id                      BIGINT        NOT NULL,
    path                    VARCHAR(1024) NOT NULL,
    content                 TEXT          NOT NULL,
    repository_id           BIGINT   NOT NULL,
    repository_version      VARCHAR(32)   NOT NULL,
    created_at              BIGINT       NOT NULL,
    updated_at              BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blueprint_repository_resource_repository_id_version_path UNIQUE (repository_id, repository_version, path)
);