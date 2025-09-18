--liquibase formatted sql

--changeset Maglitch65:dashboard_v1.3.0_20250528_100000
CREATE TABLE "t_dashboard_entity"
(
    id           BIGINT PRIMARY KEY,
    dashboard_id BIGINT       not null,
    entity_id    BIGINT       not null,
    entity_key   VARCHAR(512) not null,
    created_at   BIGINT       not null,
    updated_at   BIGINT,
    CONSTRAINT uk_dashboard_entity UNIQUE (dashboard_id, entity_id)
);


--changeset simon:dashboard_v1.3.0_20250908_173000
CREATE TABLE "t_canvas"
(
    id                      BIGINT       PRIMARY KEY,
    name                    VARCHAR(511) NOT NULL,
    attach_type             VARCHAR(255) NOT NULL,
    attach_id               VARCHAR(255) NOT NULL,
    tenant_id               VARCHAR(255) NOT NULL,
    created_at              BIGINT       NOT NULL,
    updated_at              BIGINT
);

CREATE INDEX idx_canvas_attach_type_attach_id ON t_canvas (attach_type, attach_id);

ALTER TABLE t_dashboard
    ADD COLUMN main_canvas_id BIGINT DEFAULT NULL;
ALTER TABLE t_dashboard
    ADD COLUMN description    VARCHAR(511) DEFAULT NULL;
ALTER TABLE t_dashboard
    ADD COLUMN cover_type     VARCHAR(255) DEFAULT NULL;
ALTER TABLE t_dashboard
    ADD COLUMN cover_data     VARCHAR(1023) DEFAULT NULL;

INSERT INTO t_canvas (id, name, attach_type, attach_id, tenant_id, created_at, updated_at)
    SELECT floor(random() * 1000000000000000)::BIGINT, name, 'DASHBOARD', id, tenant_id, created_at, updated_at FROM t_dashboard;

UPDATE t_dashboard
    SET main_canvas_id = t_canvas.id
    FROM t_canvas
    WHERE t_dashboard.id = t_canvas.attach_id::BIGINT
        AND t_dashboard.tenant_id = t_canvas.tenant_id;
ALTER TABLE t_dashboard_widget RENAME TO t_canvas_widget;
ALTER TABLE t_canvas_widget
    ADD COLUMN canvas_id BIGINT DEFAULT NULL;
UPDATE t_canvas_widget
    SET canvas_id = t_dashboard.main_canvas_id
    FROM t_dashboard
    WHERE t_dashboard.id = t_canvas_widget.dashboard_id
        AND t_dashboard.tenant_id = t_canvas_widget.tenant_id;

ALTER TABLE t_canvas_widget DROP COLUMN dashboard_id;

CREATE INDEX idx_canvas_widget_canvas_id ON t_canvas_widget (canvas_id);
ALTER TABLE t_dashboard_entity RENAME TO t_canvas_entity;
ALTER TABLE t_canvas_entity
    ADD COLUMN canvas_id BIGINT DEFAULT NULL;
UPDATE t_canvas_entity
    SET canvas_id = t_dashboard.main_canvas_id
    FROM t_dashboard
    WHERE t_dashboard.id = t_canvas_entity.dashboard_id;

ALTER TABLE t_canvas_entity DROP CONSTRAINT uk_dashboard_entity;
ALTER TABLE t_canvas_entity DROP COLUMN dashboard_id;
ALTER TABLE t_canvas_entity
    ADD CONSTRAINT uk_canvas_entity_canvas_id_entity_id UNIQUE (canvas_id, entity_id);
CREATE TABLE "t_dashboard_preset_cover"
(
    id                      BIGINT         PRIMARY KEY,
    name                    VARCHAR(512)   NOT NULL,
    type                    VARCHAR(255)   NOT NULL,
    data                    VARCHAR(1024)  DEFAULT NULL,
    ordered                 INT            NOT NULL
);
INSERT INTO t_dashboard_preset_cover (id, name, type, data, ordered)
    VALUES
        (10000, 'Smart City', 'DEFAULT_IMAGE', 'public/default-covers/smart-city.png', 10000),
        (20000, 'Smart Agriculture', 'DEFAULT_IMAGE', 'public/default-covers/smart-agriculture.png', 20000),
        (30000, 'Smart Building', 'DEFAULT_IMAGE', 'public/default-covers/smart-building.png', 30000),
        (40000, 'Smart', 'DEFAULT_IMAGE', 'public/default-covers/smart.png', 40000),
        (50000, 'Purple', 'DEFAULT_IMAGE', 'public/default-covers/purple.png', 50000),
        (60000, 'Blue', 'DEFAULT_IMAGE', 'public/default-covers/blue.png', 60000),
        (70000, 'Yellow', 'DEFAULT_IMAGE', 'public/default-covers/yellow.png', 70000),
        (80000, 'Green', 'DEFAULT_IMAGE', 'public/default-covers/green.png', 80000);

DROP TABLE t_dashboard_widget_template;

--changeset simon:dashboard_v1.3.0_20250918_093000
CREATE TABLE "t_canvas_device"
(
    id                      BIGINT         PRIMARY KEY,
    canvas_id               BIGINT         NOT NULL,
    device_id               BIGINT         NOT NULL,
    created_at              BIGINT         NOT NULL
);
ALTER TABLE t_canvas_device
    ADD CONSTRAINT uk_canvas_device_canvas_id_device_id UNIQUE (canvas_id, device_id);
