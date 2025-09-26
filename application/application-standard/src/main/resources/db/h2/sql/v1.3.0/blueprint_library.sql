--liquibase formatted sql

--changeset pandalxb:blueprint_library_v1.3.0_20250901_092200
CREATE TABLE `t_blueprint_library`
(
    id                      BIGINT        NOT NULL,
    type                    VARCHAR(32)   NOT NULL,
    url                     VARCHAR(512)  NOT NULL,
    branch                  VARCHAR(255)  NOT NULL,
    current_version         VARCHAR(32),
    remote_version          VARCHAR(32),
    sync_status             VARCHAR(32)   NOT NULL,
    sync_message            TEXT,
    synced_at               BIGINT,
    created_at              BIGINT        NOT NULL,
    updated_at              BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blueprint_library_type_url_branch UNIQUE (type, url, branch)
);

--changeset pandalxb:blueprint_library_v1.3.0_20250924_090000
ALTER TABLE `t_blueprint_library`
    ADD COLUMN source_type VARCHAR(32) DEFAULT 'Default';

--changeset simon:blueprint_library_v1.3.0_20250926_164500
ALTER TABLE `t_blueprint_library`
    ALTER COLUMN source_type SET NOT NULL;
