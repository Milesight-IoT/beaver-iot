--liquibase formatted sql

--changeset pandalxb:device_template_v1.3.0_20250909_132200
ALTER TABLE t_device_template
    ADD COLUMN blueprint_library_id BIGINT;

ALTER TABLE t_device_template
    ADD COLUMN blueprint_library_version VARCHAR(32);

ALTER TABLE t_device_template
    ADD COLUMN vendor VARCHAR(255);

ALTER TABLE t_device_template
    ADD COLUMN model VARCHAR(255);

ALTER TABLE t_device_template
    ADD CONSTRAINT uk_device_template_blueprint_library_id_blueprint_library_version_vendor_model_tenant_id UNIQUE (blueprint_library_id, blueprint_library_version, vendor, model, tenant_id);

ALTER TABLE t_device_template
    ALTER COLUMN tenant_id SET NOT NULL;