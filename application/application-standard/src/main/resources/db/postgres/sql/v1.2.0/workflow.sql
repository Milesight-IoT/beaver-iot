--liquibase formatted sql

--changeset zl:workflow_v1.2.0_20250307_175400
ALTER TABLE "t_flow_log"
    ADD COLUMN message VARCHAR(1000);