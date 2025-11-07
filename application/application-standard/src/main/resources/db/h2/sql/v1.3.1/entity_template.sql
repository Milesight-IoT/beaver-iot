--liquibase formatted sql

-- changeset pandalxb:entity_template_v1.3.1_20251107_144000
ALTER TABLE t_entity_template
    ADD COLUMN value_store_mod VARCHAR(255) DEFAULT 'ALL';
UPDATE t_entity_template
    SET value_store_mod = 'HISTORY' WHERE type IN ('SERVICE', 'EVENT');
