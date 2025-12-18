--liquibase formatted sql

-- changeset Maglitch65:workflow_v1.3.1_20251105_100000
-- comment: Add index on created_at column for time-series data cleanup and query optimization
CREATE INDEX IF NOT EXISTS idx_flow_log_created_at ON t_flow_log (created_at);

-- changeset chensh:workflow_v1.3.1_20251211_101000
ALTER TABLE t_flow
    ALTER COLUMN user_id SET NULL;
ALTER TABLE t_flow
    ALTER COLUMN updated_user SET NULL;
ALTER TABLE t_flow_log
    ALTER COLUMN user_id SET NULL;

-- changeset chensh:workflow_v1.3.1_20251216_101000
ALTER TABLE t_flow_history
    ALTER COLUMN user_id SET NULL;