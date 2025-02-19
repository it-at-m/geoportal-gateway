package de.swm.lhm.geoportal.gateway.resource;

import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class AbstractResourceControllerTest extends BaseIntegrationTest {
    protected final static String PUBLIC_LAYER = "myws:publiclayer";
    protected final static String PUBLIC_LAYER_NAME = "publiclayer";


    protected final static String PROTECTED_LAYER = "myws:protectedlayer";
    protected final static String PROTECTED_LAYER_NAME = "protectedlayer";
    protected final static String PROTECTED_AUTH_LEVEL_HIGH_LAYER = "myws:protectedhighlayer";
    protected final static String PROTECTED_AUTH_LEVEL_HIGH_LAYER_NAME = "protectedhighlayer";
    protected final static String UNKNOWN_LAYER_NAME = "unknownlayer";

    protected final static String STANDARD_UNIT = "myws";
    protected final static String PUBLIC_CONFIGURED_RESOURCE = "public.pdf";
    protected final static String PROTECTED_CONFIGURED_RESOURCE = "protected.pdf";
    protected final static String NOT_CONFIGURED_RESOURCE = "unknown.pdf";

    protected final static String NO_LAYER_UNIT = "nolayer";
    protected final static String STAGE = "CONFIGURATION";

    @BeforeEach
    void createDbState() {
        runSql(
                """
                               create table geoservice_product_roles_view (
                                   resource_id text,
                                   stage character varying(255),
                                   auth_level_high boolean,
                                   access_level character varying(11),
                                   geoservice_id integer,
                                   role_name character varying(256)
                               )
                        """);

        runSql(
                "insert into geoservice_product_roles_view " +
                        "(resource_id, stage, auth_level_high, access_level, geoservice_id, role_name) " +
                        " values " +
                        "('" + PUBLIC_LAYER + "', '" + Stage.CONFIGURATION + "', false, '" + AccessLevel.PUBLIC + "', 1, '" + PUBLIC_PRODUCT + "')," +
                        "('" + PROTECTED_LAYER + "', '" + Stage.CONFIGURATION + "', false, '" + AccessLevel.PROTECTED + "', 2, '" + PROTECTED_PRODUCT + "')," +
                        "('" + PROTECTED_AUTH_LEVEL_HIGH_LAYER + "', '" + Stage.CONFIGURATION + "', true, '" + AccessLevel.PROTECTED + "', 3, '" + PROTECTED_AUTH_LEVEL_HIGH_PRODUCT + "')"
        );

        runSql(
                """
                               create table  resource_product_roles_view (
                                   resource_id text,
                                   unit character varying(255),
                                   name character varying(255),
                                   auth_level_high boolean,
                                   access_level character varying(11),
                                   stage character varying(255),
                                   role_name character varying(256)
                               )
                        """);

        runSql(
                "insert into resource_product_roles_view " +
                        "(unit, name, auth_level_high, access_level, stage, role_name)" +
                        " values " +
                        "('" + STANDARD_UNIT + "', '" + PUBLIC_LAYER_NAME + "/" + PUBLIC_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PUBLIC + "', '" + STAGE + "', '" + PUBLIC_PRODUCT + "'),"  +
                        "('" + STANDARD_UNIT + "', '" + PUBLIC_LAYER_NAME + "/" + PROTECTED_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PROTECTED + "', '" + STAGE + "', '" + PROTECTED_PRODUCT + "')," +

                        "('" + STANDARD_UNIT + "', '" + PROTECTED_LAYER_NAME + "/" + PUBLIC_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PUBLIC + "', '" + STAGE + "', '" + PUBLIC_PRODUCT + "')," +
                        "('" + STANDARD_UNIT + "', '" + PROTECTED_LAYER_NAME + "/" + PROTECTED_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PROTECTED + "', '" + STAGE + "', '" + PROTECTED_PRODUCT + "')," +

                        "('" + STANDARD_UNIT + "', '" + PROTECTED_AUTH_LEVEL_HIGH_LAYER_NAME + "/" + PUBLIC_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PUBLIC + "', '" + STAGE + "', '" + PUBLIC_PRODUCT + "')," +
                        "('" + STANDARD_UNIT + "', '" + PROTECTED_AUTH_LEVEL_HIGH_LAYER_NAME + "/" + PROTECTED_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PROTECTED + "', '" + STAGE + "', '" + PROTECTED_PRODUCT + "')," +

                        "('" + STANDARD_UNIT + "', '" + UNKNOWN_LAYER_NAME + "/" + PUBLIC_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PUBLIC + "', '" + STAGE + "', '" + PUBLIC_PRODUCT + "')," +
                        "('" + STANDARD_UNIT + "', '" + UNKNOWN_LAYER_NAME + "/" + PROTECTED_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PROTECTED + "', '" + STAGE + "', '" + PROTECTED_PRODUCT + "')," +

                        "('" + NO_LAYER_UNIT + "', '" + PUBLIC_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PUBLIC + "', '" + STAGE + "', '" + PUBLIC_PRODUCT + "')," +
                        "('" + NO_LAYER_UNIT + "', '" + PROTECTED_CONFIGURED_RESOURCE + "', false, '" + AccessLevel.PROTECTED + "', '" + STAGE + "', '" + PROTECTED_PRODUCT + "')"

        );
    }

        @AfterEach
        void destroyDbState() {
            runSql("drop table geoservice_product_roles_view");
            runSql("drop table resource_product_roles_view");
        }

    }