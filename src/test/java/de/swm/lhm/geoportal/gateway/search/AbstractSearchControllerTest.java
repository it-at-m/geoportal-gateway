package de.swm.lhm.geoportal.gateway.search;

import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.SearchResultTo;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.Mockito.doReturn;


@ActiveProfiles("elasticsearch")
public abstract class AbstractSearchControllerTest extends BaseIntegrationTest {

    @SpyBean
    protected SearchService searchService;

    @Autowired
    protected SearchController searchController;

    protected final static String PUBLIC_PORTAL = "Portal_public";
    protected final static int PUBLIC_PORTAL_ID = 0;
    protected final static String PROTECTED_PORTAL = "Portal_protected";
    protected final static int PROTECTED_PORTAL_ID = 1;
    protected final static String PROTECTED_AUTH_LEVEL_HIGH_PORTAL = "Portal_protectedhigh";
    protected final static int PROTECTED_AUTH_LEVEL_HIGH_PORTAL_ID = 2;
    protected final static int NON_EXISTING_PORTAL_ID = -1;
    protected final static int UNIT_ID = 100;
    protected final static String UNIT = "unit";
    protected final static String PORTAL_URL = "portal_url";
    protected final static String SEARCH_INDEX_GEO_DATA = "geoData";
    protected final static String SEARCH_INDEX_GEO_SERVICE = "geoService";
    protected final static String SEARCH_INDEX_META_DATA = "metaData";
    protected final static String SEARCH_STRING = "searchString";
    protected final static int MAX_RESULT_AMOUNT = 1;

    SearchResultTo geoDataSearchResult = SearchResultTo.builder()
            .id(SEARCH_INDEX_GEO_DATA)
            .type("Geodaten")
            .coordinate(List.of(5333536.976132349, 687716.6843805218))
            .displayValue("Parkhaus Audi Dome (opendata_parkhaeuser)")
            .layerId("764197")
            .layerTitle("opendata_parkhaeuser")
            .geoDataValue("Parkhaus Audi Dome")
            .build();

    SearchResultTo addressSearchResult = SearchResultTo.builder()
            .id("fdc53678-0cef-4349-a662-a1d49dd49ee2")
            .type("Adresse")
            .id("fdc53678-0cef-4349-a662-a1d49dd49ee2")
            .coordinate(List.of(5333874.0, 691413.0))
            .displayValue("Klenzestraße 59, 80469 München")
            .streetName("Klenzestraße")
            .city("München")
            .zipCode("80469")
            .streetNameComplete("Klenzestraße 59")
            .build();

    @Autowired
    protected DatabaseClient client;

    @BeforeEach
    void createDbState() {

        runSql(
                """
                create table portal_product_roles_view (
                    resource_id text,
                    stage character varying(255),
                    auth_level_high boolean,
                    access_level character varying(11),
                    role_name character varying(256)
                );
                
                create table t_portal (
                    id integer,
                    stage character varying(255) default 'CONFIGURATION',
                    name character varying(255),
                    title character varying(255),
                    url character varying(255),
                    unitId integer,
                    unit character varying(255),
                    access_level character varying(11),
                    auth_level_high boolean DEFAULT false,
                    search_index_geo_data character varying(255),
                    search_index_geo_service character varying(255),
                    search_index_meta_data character varying(255)
                )
                """);


        runSql(
                "insert into portal_product_roles_view " +
                        "(resource_id, stage, auth_level_high, access_level, role_name) " +
                        " values " +
                        "('" + PUBLIC_PORTAL + "', '" + Stage.CONFIGURATION + "', false, '" + AccessLevel.PUBLIC + "', '" + PUBLIC_PRODUCT + "')," +
                        "('" + PROTECTED_PORTAL + "', '" + Stage.CONFIGURATION + "', false, '" + AccessLevel.PROTECTED + "', '" + PROTECTED_PRODUCT + "')," +
                        "('" + PROTECTED_AUTH_LEVEL_HIGH_PORTAL + "', '" + Stage.CONFIGURATION + "', true, '" + AccessLevel.PROTECTED + "', '" + PROTECTED_AUTH_LEVEL_HIGH_PRODUCT + "'); " +
                        "insert into t_portal "
                        + "(id, name, title, url, unitId, unit, access_level, auth_level_high, search_index_geo_data, search_index_geo_service, search_index_meta_data) "
                        + " values "
                        + "('" + PUBLIC_PORTAL_ID + "', '" + PUBLIC_PORTAL + "', '" + PUBLIC_PORTAL + "', '" + PORTAL_URL + "', '"
                        + UNIT_ID + "', '" + UNIT + "', '" + AccessLevel.PUBLIC + "', '" + false + "', '"
                        + SEARCH_INDEX_GEO_DATA + "', '" + SEARCH_INDEX_GEO_SERVICE + "', '" + SEARCH_INDEX_META_DATA + "'),"

                        + "('" + PROTECTED_PORTAL_ID + "', '" + PROTECTED_PORTAL + "', '" + PROTECTED_PORTAL + "', '" + PORTAL_URL + "', '"
                        + UNIT_ID + "', '" + UNIT + "', '" + AccessLevel.PUBLIC + "', '" + false + "', '"
                        + SEARCH_INDEX_GEO_DATA + "', '" + SEARCH_INDEX_GEO_SERVICE + "', '" + SEARCH_INDEX_META_DATA + "'),"

                        + "('" + PROTECTED_AUTH_LEVEL_HIGH_PORTAL_ID + "', '" + PROTECTED_AUTH_LEVEL_HIGH_PORTAL + "', '" + PROTECTED_AUTH_LEVEL_HIGH_PORTAL + "', '" + PORTAL_URL + "', '"
                        + UNIT_ID + "', '" + UNIT + "', '" + AccessLevel.PROTECTED + "', '" + false + "', '"
                        + SEARCH_INDEX_GEO_DATA + "', '" + SEARCH_INDEX_GEO_SERVICE + "', '" + SEARCH_INDEX_META_DATA + "')"
        );

        doReturn(Flux.just(geoDataSearchResult)).when(searchService).searchGeoData(SEARCH_STRING, SEARCH_INDEX_GEO_DATA);
        doReturn(Flux.just(addressSearchResult)).when(searchService).searchAddress(SEARCH_STRING, MAX_RESULT_AMOUNT);
    }

    @AfterEach
    void destroyDbState() {
        runSql("""
                drop table portal_product_roles_view;
                drop table t_portal;
                """);
    }
}
