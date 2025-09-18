--liquibase formatted sql

--changeset pandalxb:blueprint_library_address_v1.3.0_20250916_151500
CREATE TABLE t_blueprint_library_address
(
    id                      BIGINT        NOT NULL,
    type                    VARCHAR(32)   NOT NULL,
    home                    VARCHAR(512)  NOT NULL,
    branch                  VARCHAR(255)  NOT NULL,
    active                  BOOLEAN       NOT NULL,
    tenant_id               VARCHAR(255)  DEFAULT 'default',
    created_at              BIGINT        NOT NULL,
    updated_at              BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blueprint_library_address_home_branch_tenant_id UNIQUE (home, branch, tenant_id)
);