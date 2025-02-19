CREATE TABLE IF NOT EXISTS t_product_portal
(
    product_id integer,
    portal_id integer
);

INSERT INTO t_product_portal
(
    product_id,
    portal_id
)
VALUES
    (1, 1),
    (1, 2),
    (1, 3);

CREATE TABLE IF NOT EXISTS t_unit
(
    id integer NOT NULL,
    abbreviation character varying(255),
    contact_person character varying(255),
    group_mail character varying(255),
    mail character varying(255),
    phone character varying(255),
    mobile character varying(255),
    organisational_unit character varying(255),
    unit_name character varying(255),
    active boolean
    );

INSERT INTO t_unit
(id, unit_name)
VALUES
    (1, 'firstUnit'),
    (3, 'thirdUnit');

CREATE TABLE IF NOT EXISTS t_portal
(
    id integer,
    name character varying(255),
    stage character varying(255),
    title character varying(255),
    updated_timestamp timestamp without time zone DEFAULT now(),
    unit_id integer,
    header_image bytea,
    headline character varying(255),
    in_transport boolean DEFAULT false,
    internal_identifier character varying(255),
    overview_map_geo_service integer,
    feature_count integer DEFAULT 42,
    in_transport_time_start timestamp without time zone,
    header_image_file_name character varying(255),
    access_level character varying(11) DEFAULT 'PUBLIC'::character varying(11),
    auth_level_high boolean DEFAULT false,
    metadata_id integer,
    background_tree_node_id integer,
    main_tree_node_id integer,
    time_machine_id integer,
    search_index_geo_data character varying(255),
    search_index_geo_service character varying(255),
    search_index_meta_data character varying(255),
    additional_information_id integer,
    printtemplate_id integer,
    wfs_filter_id integer,
    promote_request_time timestamp without time zone,
    demote_request_time timestamp without time zone,
    start_center_x integer DEFAULT 691603,
    start_center_y integer DEFAULT 5334760,
    start_zoom_level integer DEFAULT 5,
    css text,
    mouse_hover boolean DEFAULT false,
    alert text,
    statistics boolean DEFAULT false,
    tree_type character varying(30) DEFAULT 'CUSTOM'::character varying,
    wfst_editable boolean DEFAULT false,
    button_3d boolean DEFAULT false,
    wfs_filter_json text,
    attributions text
    );

INSERT INTO t_portal
(id, name, stage, unit_id, search_index_geo_data, search_index_geo_service, search_index_meta_data)
VALUES
    (1, 'portal1', 'CONFIGURATION', 1, 'ebab0b3f-ec48-443b-868b-91a5836f4438', 'ebab0b3f-ec48-443b-868b-91a5836f4438', 'ebab0b3f-ec48-443b-868b-91a5836f4438'),
    (2, 'portal2', 'CONFIGURATION', 2, null, null, null),
    (3, 'portal3', 'CONFIGURATION', 3, null ,null, null);