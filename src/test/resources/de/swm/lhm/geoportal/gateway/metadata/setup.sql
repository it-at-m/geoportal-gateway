CREATE TABLE IF NOT EXISTS t_metadata
(
    id integer NOT NULL,
    unit character varying(255),
    service character varying(255),
    metadataid character varying(255),
    metadatatitle character varying(255),
    valid boolean,
    updated_timestamp timestamp without time zone NOT NULL DEFAULT now(),
    stage character varying(40),
    metadata_exists boolean NOT NULL DEFAULT true,
    access_level character varying(11),
    CONSTRAINT t_metadata_pkey PRIMARY KEY (id),
    CONSTRAINT t_metadata_stage_metadataid_ukey UNIQUE (stage, metadataid)
    );

INSERT INTO t_metadata
(id, metadataid)
VALUES
    (1, 'cc8f7e3c-ee50-4714-ae25-8da5384ebfc2');