--liquibase formatted sql

--changeset pandalxb:blueprint_library_subscription_v1.3.0_20250919_100000
CREATE TABLE `t_blueprint_library_subscription`
(
    id                      BIGINT        NOT NULL,
    library_id              BIGINT        NOT NULL,
    library_version         VARCHAR(32)   NOT NULL,
    active                  BOOLEAN       NOT NULL,
    tenant_id               VARCHAR(255)  NOT NULL,
    created_at              BIGINT        NOT NULL,
    updated_at              BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blueprint_library_subscription_library_id_tenant_id UNIQUE (library_id, tenant_id)
);

--changeset pandalxb:blueprint_library_subscription_v1.3.0_20250924_164700
CREATE INDEX idx_blueprint_library_subscription_active_tenant_id ON `t_blueprint_library_subscription` (active, tenant_id);
CREATE INDEX idx_blueprint_library_subscription_tenant_id ON `t_blueprint_library_subscription` (tenant_id);