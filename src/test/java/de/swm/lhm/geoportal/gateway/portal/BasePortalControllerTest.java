package de.swm.lhm.geoportal.gateway.portal;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


public abstract class BasePortalControllerTest extends BaseIntegrationTest {

    protected final static String PUBLIC_PORTAL = "public-portal";
    protected final static String PROTECTED_PORTAL = "protected-portal";
    protected final static String PROTECTED_AUTH_LEVEL_HIGH_PORTAL = "protected-high-portal";
    protected final static String RESTRICTED_PORTAL = "restricted-portal";

    @BeforeEach
    void setUp() {

        runSql(
                """
                create table portal_product_roles_view
                (
                    resource_id text,
                    stage character varying(255),
                    auth_level_high boolean,
                    access_level character varying(11),
                    role_name character varying(256)
                )
                """);

        runSql(

                "insert into portal_product_roles_view " +
                        "(resource_id, stage, auth_level_high, access_level, role_name) " +
                        " values " +
                        "('" + PUBLIC_PORTAL + "', '"+ Stage.CONFIGURATION +"', false, '"+ AccessLevel.PUBLIC +"', '"+PUBLIC_PRODUCT+"')," +
                        "('" + PROTECTED_PORTAL + "', '"+ Stage.CONFIGURATION +"', false, '"+ AccessLevel.PROTECTED +"', '"+PROTECTED_PRODUCT+"')," +
                        "('" + PROTECTED_AUTH_LEVEL_HIGH_PORTAL + "', '"+ Stage.CONFIGURATION +"', true, '"+ AccessLevel.PROTECTED +"', '"+PROTECTED_AUTH_LEVEL_HIGH_PRODUCT+"')," +
                        "('" + RESTRICTED_PORTAL + "', '"+ Stage.CONFIGURATION +"', false, '"+ AccessLevel.RESTRICTED +"', '"+RESTRICTED_PRODUCT+"')"
        );
    }

    @AfterEach
    void tearDown() {
        runSql("drop table portal_product_roles_view");
    }


}