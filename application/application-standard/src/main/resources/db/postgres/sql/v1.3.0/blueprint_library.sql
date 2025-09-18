--liquibase formatted sql

--changeset pandalxb:blueprint_library_v1.3.0_20250901_092200
CREATE TABLE "t_blueprint_library"
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
    CONSTRAINT uk_blueprint_library_home_branch UNIQUE (home, branch)
);

--changeset pandalxb:blueprint_library_v1.3.0_20250917_084400
ALTER TABLE t_blueprint_library
    ADD COLUMN type VARCHAR(32);

ALTER TABLE t_blueprint_library
    DROP CONSTRAINT uk_blueprint_library_home_branch;

ALTER TABLE t_blueprint_library
    ADD CONSTRAINT uk_blueprint_library_type_home_branch UNIQUE (type, home, branch);

ALTER TABLE t_blueprint_library
    ALTER COLUMN synced_at DROP NOT NULL;

ALTER TABLE t_blueprint_library
    ALTER COLUMN remote_version DROP NOT NULL;

ALTER TABLE t_blueprint_library
    ADD COLUMN sync_message TEXT;