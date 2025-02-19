CREATE TABLE IF NOT EXISTS t_product_layer
(
    product_id integer NOT NULL,
    layer_id character varying(255) NOT NULL,
    layer_type character varying(255) NOT NULL
    );

DELETE FROM t_product_layer;

INSERT INTO t_product_layer
(product_id, layer_id, layer_type)
VALUES
    (1, 'eca0dc07-aa9c-4053-852c-d7ba02ed4e8f', 'STA'),
    (1, 'workspace2:layer2', 'WMS'),
    (1, '3473efa6-25aa-4470-b876-1e9a8c53016a', 'GEN');

CREATE TABLE IF NOT EXISTS t_plugin_sta_sensor
(
    id character varying(36) NOT NULL,
    name character varying(100),
    url character varying(100),
    version character varying(10),
    epsg character varying(255),
    filter character varying(2000),
    expand character varying(2000),
    extent_only boolean,
    gfi_query text,
    attributions character varying(1000),
    scale_min integer DEFAULT 500,
    scale_max integer DEFAULT 400000,
    cluster_distance integer DEFAULT 0,
    metadata_id integer,
    style_id integer,
    updated_timestamp timestamp without time zone DEFAULT now(),
    updated_by_user character varying(255),
    mouse_hover character varying(2000),
    legend text DEFAULT 'true'::text,
    stage character varying(255) NOT NULL DEFAULT 'CONFIGURATION'::character varying,
    stageless_id character varying(255) NOT NULL
);

DELETE FROM t_plugin_sta_sensor;

INSERT INTO t_plugin_sta_sensor
    (id, name, url, stageless_id, stage)
VALUES
    ('2e493acd-1551-4bcb-b8bb-b96f05290b87', 'layer1', 'https://sensor.frost.somewhere', 'eca0dc07-aa9c-4053-852c-d7ba02ed4e8f', 'CONFIGURATION');
               