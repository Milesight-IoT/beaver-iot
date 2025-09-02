--liquibase formatted sql

--changeset pandalxb:blueprint_repository_v1.3.0_20250901_092200
CREATE TABLE `t_blueprint_repository`
(
    id                      BIGINT        NOT NULL,
    home                    VARCHAR(512)  NOT NULL,
    branch                  VARCHAR(255)  NOT NULL,
    current_version         VARCHAR(32),
    remote_version          VARCHAR(32)   NOT NULL,
    sync_status             VARCHAR(32)   NOT NULL,
    synced_at               BIGINT       NOT NULL,
    created_at              BIGINT       NOT NULL,
    updated_at              BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blueprint_repository_home_branch UNIQUE (home, branch)
);