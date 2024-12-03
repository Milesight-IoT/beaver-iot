--liquibase formatted sql

--changeset loong:user_v1.0.0_20241024_095400
CREATE TABLE `t_user`
(
    id         BIGINT PRIMARY KEY,
    email      VARCHAR(255) not null,
    email_hash VARCHAR(255) not null,
    nickname   VARCHAR(255) not null,
    password   VARCHAR(255) not null,
    preference VARCHAR(255),
    created_at BIGINT       not null,
    updated_at BIGINT,
    CONSTRAINT uk_email UNIQUE (email)
);