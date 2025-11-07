--liquibase formatted sql

-- changeset Maglitch65:entity_v1.3.1_20251105_100000
-- comment: Add index on timestamp column for time-series data cleanup and query optimization
CREATE INDEX IF NOT EXISTS idx_entity_history_timestamp ON "t_entity_history" (timestamp);