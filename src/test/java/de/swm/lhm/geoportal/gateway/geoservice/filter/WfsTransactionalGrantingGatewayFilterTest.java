package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.geoservice.AbstractGeoServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

class WfsTransactionalGrantingGatewayFilterTest extends AbstractGeoServiceTest {

    @BeforeEach
    void createWfsTDbState() {
        runSql("""
                                
                CREATE TABLE t_portal
                (
                    id integer NOT NULL,
                    name character varying(255) NOT NULL,
                    stage character varying(255),
                    title character varying(255) NOT NULL,
                    updated_timestamp timestamp without time zone NOT NULL DEFAULT now(),
                    unit_id integer,
                    header_image bytea,
                    headline character varying(255),
                    in_transport boolean NOT NULL DEFAULT false,
                    internal_identifier character varying(255) NOT NULL,
                    overview_map_geo_service integer,
                    feature_count integer DEFAULT 42,
                    in_transport_time_start timestamp without time zone,
                    header_image_file_name character varying(255) ,
                    access_level character varying(11) NOT NULL DEFAULT 'PUBLIC'::character varying(11),
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
                    tree_type character varying(30),
                    wfst_editable boolean DEFAULT false,
                    button_3d boolean NOT NULL DEFAULT false,
                    wfs_filter_json text,
                    attributions text
                )
                """);
        runSql("""
                CREATE TABLE t_wfst
                (
                    portal_id integer NOT NULL,
                    wfst_layer character varying(255) NOT NULL,
                    geometry character varying(16)
                )
                """);
        runSql("""
                insert into t_portal (id, name, stage, title, internal_identifier, wfst_editable)
                    values (1, 'p1', 'CONFIGURATION', 'p1', '655268af-da6b-4e69-96fd-8d873639dc1e', true)
                """);
        runSql("insert into t_wfst (portal_id, wfst_layer, geometry) values "
                + "            (1, '" + PROTECTED_LAYER + "', 'POINT')"
        );
    }

    @AfterEach
    void destroyWfsTDbState() {
        runSql("drop table t_portal");
        runSql("drop table t_wfst");
    }

    WebTestClient.ResponseSpec runTransaction(String layerName) {
        return webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_PRODUCT, PROTECTED_PRODUCT2))
                )
                .post()
                .uri("/geoserver/wfs")
                .headers(httpHeaders -> {
                    httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
                })
                .bodyValue("""
                        <Transaction xmlns="http://www.opengis.net/wfs" service="WFS" version="1.1.0"
                                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                     xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd">
                                     """
                        + "<Update typeName=\"" + layerName + "\" xmlns:play=\"playgound\">"
                        + """
                                <Property>
                                    <Name>play:shape</Name>
                                    <Value>
                                        <Point xmlns="http://www.opengis.net/gml" srsName="EPSG:25832">
                                            <pos srsDimension="2">686096.4609374998 5334186.0349127175</pos>
                                        </Point>
                                    </Value>
                                </Property>
                                <Filter xmlns="http://www.opengis.net/ogc">
                                    <FeatureId fid="opendata_parkhaeuser.3"/>
                                </Filter>
                            </Update>
                        </Transaction>
                        """)
                .exchange();
    }

    @Test
    void editWfsTEnabledLayer() {
        mockGeoServerPost();
        WebTestClient.ResponseSpec response = runTransaction(PROTECTED_LAYER);
        expectGeoServerResponseOk(response);
    }

    @Test
    void editNonWfsTEnabledLayer() {
        mockGeoServerPost();
        WebTestClient.ResponseSpec response = runTransaction(PROTECTED_LAYER2);
        response.expectStatus().isForbidden();
    }
}
