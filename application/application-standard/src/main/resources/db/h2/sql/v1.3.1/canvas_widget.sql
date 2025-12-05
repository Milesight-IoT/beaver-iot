--liquibase formatted sql

--changeset pandalxb:canvas_widget_v1.3.1_20251205_092500
ALTER TABLE t_canvas_widget
    ALTER COLUMN user_id SET NULL;

ALTER TABLE t_canvas_widget
    ALTER COLUMN user_id DROP DEFAULT;