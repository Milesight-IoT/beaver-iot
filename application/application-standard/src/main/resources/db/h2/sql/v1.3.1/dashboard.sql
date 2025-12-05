--liquibase formatted sql

--changeset pandalxb:dashboard_v1.3.1_20251205_092500
ALTER TABLE t_dashboard
    ALTER COLUMN user_id SET NULL;