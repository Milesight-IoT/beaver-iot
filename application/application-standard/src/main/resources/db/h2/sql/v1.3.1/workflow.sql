-- changeset Maglitch65:workflow_v1.3.1_20251105_100000
-- comment: Add index on created_at column for time-series data cleanup and query optimization
CREATE INDEX IF NOT EXISTS idx_flow_log_created_at ON t_flow_log (created_at);
