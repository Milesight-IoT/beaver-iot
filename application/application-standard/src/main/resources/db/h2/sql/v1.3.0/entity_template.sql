--liquibase formatted sql

--changeset pandalxb:entity_template_v1.3.0_20250820_084500
CREATE TABLE `t_entity_template`
(
    id                      BIGINT       NOT NULL,
    "key"                   VARCHAR(512) NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    type                    VARCHAR(255) NOT NULL,
    access_mod              VARCHAR(255),
    parent                  varchar(512),
    value_type              varchar(255) NOT NULL,
    value_attribute         TEXT,
    description             TEXT,
    visible                 BOOLEAN      DEFAULT true NOT NULL,
    tenant_id               VARCHAR(255) DEFAULT 'default',
    user_id                 BIGINT,
    created_at              BIGINT       NOT NULL,
    updated_at              BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_entity_template_key_tenant_id UNIQUE ("key", tenant_id)
);

CREATE INDEX idx_entity_template_tenant_id ON `t_entity_template` (tenant_id);
CREATE INDEX idx_entity_template_parent ON `t_entity_template` (parent);

--changeset pandalxb:entity_template_v1.3.0_20250918_150000
ALTER TABLE `t_entity_template`
    ALTER COLUMN tenant_id SET NOT NULL;