--liquibase formatted sql

-- changeset pandalxb:entity_v1.3.1_20251107_130700
ALTER TABLE t_entity
    ADD COLUMN value_store_mod VARCHAR(255) DEFAULT 'ALL';
UPDATE t_entity
    SET value_store_mod = 'HISTORY' WHERE type IN ('SERVICE', 'EVENT');