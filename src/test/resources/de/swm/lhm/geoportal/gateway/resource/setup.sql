CREATE TABLE IF NOT EXISTS t_file_resource
(
    id integer NOT NULL,
    unit character varying(255) NOT NULL,
    portalcontent text,
    mime_type character varying(255),
    name character varying(255) NOT NULL,
    updated_timestamp timestamp without time zone NOT NULL DEFAULT now(),
    file_resource_type character varying(255),
    file_exists boolean DEFAULT true,
    access_level character varying(11) NOT NULL DEFAULT 'PUBLIC'::character varying,
    auth_level_high boolean DEFAULT false,
    stage character varying(255),
    stageless_id character varying(36),
    CONSTRAINT t_file_resource_pkey PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS t_product_fileresource
(
    product_id integer NOT NULL,
    fileresource_id integer NOT NULL
);

INSERT INTO t_product_fileresource
(product_id, fileresource_id)
VALUES
    (1, 1),
    (1, 2),
    (1, 3)
;

INSERT INTO t_file_resource
(id, unit, name)
VALUES
    (1, 'test', 'layer\column\1.pdf'),
    (2, 'test', '/layer/column/2.pdf')