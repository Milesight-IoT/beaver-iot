--liquibase formatted sql

--changeset Maglitch65:blueprint_v1.3.0_20250901_100000
CREATE TABLE t_blueprint
(
    id          BIGINT PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    description TEXT,
    chart       TEXT         NOT NULL,
    created_at  BIGINT       NOT NULL,
    created_by  VARCHAR(255) NOT NULL,
    updated_at  BIGINT,
    updated_by  VARCHAR(255)
);

--changeset Maglitch65:blueprint_v1.3.0_20250901_100001
CREATE TABLE t_blueprint_resource
(
    id            BIGINT PRIMARY KEY,
    resource_type VARCHAR(255) NOT NULL,
    resource_id   VARCHAR(255) NOT NULL,
    blueprint_id  BIGINT       NOT NULL,
    tenant_id     VARCHAR(255) NOT NULL,
    managed       BOOLEAN DEFAULT TRUE,
    created_at    BIGINT       NOT NULL,
    created_by    VARCHAR(255) NOT NULL,
    CONSTRAINT uk_blueprint_resource UNIQUE (blueprint_id, tenant_id, resource_type, resource_id)
);
CREATE INDEX idx_tenant_resource ON t_blueprint_resource (tenant_id, resource_type, resource_id);
