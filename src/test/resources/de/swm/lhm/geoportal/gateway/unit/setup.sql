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
