--liquibase formatted sql

--changeset pandalxb:device_template_v1.2.0_20250514_083900
CREATE TABLE t_device_template (
                                   id BIGINT NOT NULL,
                                   key VARCHAR(512) NOT NULL,
                                   name VARCHAR(255) NOT NULL,
                                   integration VARCHAR(127) NOT NULL,
                                   identifier VARCHAR(127) NOT NULL,
                                   content VARCHAR(1048576) DEFAULT NULL,
                                   additional_data VARCHAR(1024) DEFAULT NULL,
                                   description VARCHAR(1024) DEFAULT NULL,
                                   tenant_id VARCHAR(255) DEFAULT 'default',
                                   user_id BIGINT,
                                   created_at BIGINT NOT NULL,
                                   updated_at BIGINT NOT NULL,
                                   PRIMARY KEY (id),
                                   CONSTRAINT uk_device_template_key_tenant_id UNIQUE (key, tenant_id)
);

CREATE INDEX idx_device_template_integration ON t_device_template (integration);
CREATE INDEX idx_device_template_tenant_id ON t_device_template (tenant_id);