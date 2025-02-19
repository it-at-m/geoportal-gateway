CREATE TABLE IF NOT EXISTS t_product_layer
(
    product_id integer NOT NULL,
    layer_id character varying(255) NOT NULL,
    layer_type character varying(255) NOT NULL
);

INSERT INTO t_product_layer
(product_id, layer_id, layer_type)
VALUES
(1, '43b3a4bb-340a-4398-8ecb-7993170dbf40', 'GEN'),
(1, '199d4e29-7449-4a63-b041-31ea55b21f23', 'OTHER'),
(2, '199d4e29-7449-4a63-b041-31ea55b21f23', 'GEN');

CREATE TABLE IF NOT EXISTS t_module_generic
(
    id character varying(36),
    name character varying(100),
    json text,
    stageless_id character varying(36),
    stage character varying(255) NOT NULL DEFAULT 'CONFIGURATION'::character varying,
    updated_timestamp timestamp without time zone DEFAULT now(),
    updated_by_user character varying(255),
    CONSTRAINT t_module_generic_pkey PRIMARY KEY (id)
);

INSERT INTO t_module_generic
(id, name, stageless_id, stage, json)
VALUES
(1, 'GEN-43b3a4bb', '43b3a4bb-340a-4398-8ecb-7993170dbf40', 'CONFIGURATION', '{"url": "test"}'),
(2, 'GEN-199d4e29', '199d4e29-7449-4a63-b041-31ea55b21f23', 'CONFIGURATION', '{');