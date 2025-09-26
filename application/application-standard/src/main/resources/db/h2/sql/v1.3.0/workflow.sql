--liquibase formatted sql

--changeset simon:dashboard_v1.3.0_20250923_093000
ALTER TABLE `t_flow`
    ADD COLUMN additional_data VARCHAR(1023) DEFAULT NULL;
