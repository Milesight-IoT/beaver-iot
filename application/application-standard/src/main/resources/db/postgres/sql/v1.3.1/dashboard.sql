--liquibase formatted sql

--changeset pandalxb:dashboard_v1.3.1_20251205_092500
ALTER TABLE t_dashboard
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE t_dashboard
    ALTER COLUMN user_id DROP DEFAULT;

--changeset pandalxb:dashboard_v1.3.1_20251209_085700
ALTER TABLE t_dashboard
    ADD COLUMN attributes TEXT;