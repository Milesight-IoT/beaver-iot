--liquibase formatted sql

--changeset simon:entity_v1.2.1_20250522_093000
DELETE FROM t_entity_latest
WHERE id NOT IN (
        SELECT id
        FROM (
            SELECT id, ROW_NUMBER() OVER (PARTITION BY entity_id ORDER BY id DESC) AS rn
            FROM t_entity_latest
        ) t
        WHERE t.rn = 1
);

DROP INDEX IF EXISTS idx_entity_latest_entity_id;

alter table t_entity_latest add constraint uk_entity_latest_entity_id unique(entity_id);

CREATE INDEX idx_entity_history_entity_id_timestamp ON t_entity_history (entity_id, timestamp);
