CREATE TABLE t_unit (
                        id INTEGER NOT NULL,
                        name VARCHAR(100)
);

CREATE TABLE t_metadata (
                            id INTEGER NOT NULL,
                            metadataid VARCHAR(100)
);

CREATE TABLE t_module_generic (
                                  id varchar(36) NOT NULL,
                                  name VARCHAR(100) ,
                                  json TEXT,
                                  stageless_id VARCHAR(100),
                                  stage VARCHAR(100)
);

CREATE TABLE t_portal (
                          id INTEGER NOT NULL,
                          name VARCHAR(100),
                          title VARCHAR(100),
                          unit_id INTEGER,
                          access_level VARCHAR(11),
                          auth_level_high boolean,
                          search_index_geo_data VARCHAR(100),
                              search_index_geo_service VARCHAR(100)

);

CREATE TABLE geoservice_product_roles_view (
                                               resource_id VARCHAR(100),
                                               stage VARCHAR(100),
                                               access_level VARCHAR(11),
                                               auth_level_high BOOLEAN,
                                               role_name VARCHAR(255)
);

CREATE TABLE t_product (
                           id INTEGER NOT NULL,
                           name VARCHAR(100) ,
                           title VARCHAR(100) ,
                           description VARCHAR(100) ,
                           license VARCHAR(100),
                           stage VARCHAR(100),
                           metadata_id INTEGER,
                           unit_id INTEGER,
                           header_image_file_name VARCHAR(100),
                           header_image BYTEA,
                           access_level VARCHAR(11),
                           auth_level_high BOOLEAN,
                           role_name VARCHAR(255)

);

CREATE TABLE t_file_resource (
                                 id INTEGER,
                                 name VARCHAR(100),
                                 unit VARCHAR(100)
);

CREATE TABLE t_plugin_sta_sensor (
                                     id VARCHAR(100) ,
                                     name VARCHAR(100),
                                     url VARCHAR(100),
                                     stageless_id VARCHAR(255),
                                     stage VARCHAR(100)
);

CREATE TABLE t_style_preview (id VARCHAR(100));


