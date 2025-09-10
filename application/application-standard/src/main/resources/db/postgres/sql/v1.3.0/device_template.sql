--liquibase formatted sql

--changeset pandalxb:device_template_v1.3.0_20250909_132200
ALTER TABLE t_device_template
    ADD COLUMN vendor VARCHAR(255);

ALTER TABLE t_device_template
    ADD COLUMN model VARCHAR(255);

ALTER TABLE t_device_template
    ADD CONSTRAINT uk_device_template_vendor_model_tenant_id UNIQUE (vendor, model, tenant_id);