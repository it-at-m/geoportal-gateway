CREATE TABLE IF NOT EXISTS t_product_layer
(
    product_id integer NOT NULL,
    layer_id character varying(255) NOT NULL,
    layer_type character varying(255) NOT NULL
    );

INSERT INTO t_product_layer
(product_id, layer_id, layer_type)
VALUES
    (1, 'workspace1:layer1', 'WMS_WFS'),
    (1, 'workspace2:layer2', 'WMS'),
    (1, 'workspace3:layer3', 'WFS'),
    (1, 'workspace4:layer4', 'WMTS'),
    (1, 'workspace5:layer5', 'OTHER'),
    (1, 'eca0dc07-aa9c-4053-852c-d7ba02ed4e8f', 'STA'),
    (1, 'e376a17f-3512-4513-82bd-108babdc00a1', 'GEN');

CREATE TABLE IF NOT EXISTS t_geoservice
(
    id integer NOT NULL,
    metadata_id integer,
    name character varying(255) NOT NULL,
    title character varying(255),
    stage character varying(255) NOT NULL,
    updated_timestamp timestamp without time zone DEFAULT now(),
    commissionedby character varying(255),
    scalemin integer DEFAULT 1,
    scalemax integer DEFAULT 1000000000,
    printscalemin integer DEFAULT 1,
    printscalemax integer DEFAULT 1000000000,
    workspace character varying(255),
    search_config_id integer,
    independent boolean DEFAULT false,
    valid boolean DEFAULT true,
    single_tile boolean DEFAULT true,
    copyright character varying(255) DEFAULT 'GSM'::character varying,
    service_exists boolean DEFAULT true,
    cache_setting_id integer,
    cached boolean,
    access_level character varying(11) DEFAULT 'PUBLIC'::character varying(11),
    auth_level_high boolean DEFAULT false,
    cache_queued boolean DEFAULT false,
    queryable boolean DEFAULT false,
    namespace character varying(255),
    tile_size_x integer DEFAULT 512,
    tile_size_y integer DEFAULT 512,
    mouse_hover character varying(2000),
    legend text DEFAULT 'true'::text,
    vector_style_id character varying(40)
    );

INSERT INTO t_geoservice
(id, name, workspace, stage)
VALUES
    (1, 'layer1', 'workspace1', 'CONFIGURATION'),
    (2, 'layer2', 'workspace2', 'CONFIGURATION'),
    (3, 'layer3', 'workspace3', 'CONFIGURATION'),
    (4, 'layer4', 'workspace4', 'CONFIGURATION');

CREATE TABLE IF NOT EXISTS t_servicetype
(
    name character varying(255) NOT NULL,
    geoservice_id integer NOT NULL
    );


INSERT INTO t_servicetype
(name, geoservice_id)
VALUES
    ('WMS', 1),
    ('WFS', 1),
    ('WMTS', 1),
    ('WMS', 2),
    ('WFS', 3),
    ('WMTS', 4);