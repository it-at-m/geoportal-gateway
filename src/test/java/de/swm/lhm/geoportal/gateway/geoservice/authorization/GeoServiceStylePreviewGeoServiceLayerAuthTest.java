package de.swm.lhm.geoportal.gateway.geoservice.authorization;

import de.swm.lhm.geoportal.gateway.geoservice.AbstractGeoServiceTest;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import de.swm.lhm.geoportal.gateway.style_preview.StylePreviewProperties;
import de.swm.lhm.geoportal.gateway.style_preview.model.StylePreviewGeoServiceLayer;
import de.swm.lhm.geoportal.gateway.style_preview.repository.StylePreviewGeoServiceLayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.List;

import static de.swm.lhm.geoportal.gateway.base_classes.SessionMutator.sessionBuilder;
import static de.swm.lhm.geoportal.gateway.base_classes.SessionMutator.sessionMutator;


@TestPropertySource(properties = {
        "geoportal.gateway.style-preview.endpoint=style-preview"
})
public class GeoServiceStylePreviewGeoServiceLayerAuthTest extends AbstractGeoServiceTest {

    @Autowired
    private StylePreviewGeoServiceLayerRepository repository;

    public static final String STYLE_PREVIEW_ID_PROTECTED_LAYER = "sp-prot-layer";
    public static final String STYLE_PREVIEW_ID_PROTECTED_BACKGROUND_LAYER = "sp-prot-bg-layer";
    public static final String STYLE_PREVIEW_ID_RESTRICTED_LAYER = "sp-rest-layer";

    @BeforeEach
    void createStylePreviewDbState() {
        // somewhat hacky db initialization
        runSql(
                """
                create table t_style_preview (
                    id character varying(36),
                    layer_id character varying(255)
                );

                create table t_style_preview_background_layer (
                    id integer,
                    style_preview_id character varying(36),
                    layer_id character varying(255)
                );
                
                create table t_geoservice (
                    id integer,
                    name character varying(255),
                    stage character varying(255),
                    workspace character varying(255)
                )
                """
        );

        List<String> protectedLayerWorkspaceAndName = List.of(PROTECTED_LAYER.split(":"));
        int protectedLayerId = 1;
        List<String> protectedLayer2WorkspaceAndName = List.of(PROTECTED_LAYER2.split(":"));
        int protectedLayer2Id = 2;
        List<String> restrictedLayerWorkspaceAndName = List.of(RESTRICTED_LAYER.split(":"));
        int restrictedLayerId = 3;

        runSql(
                "insert into t_style_preview " +
                        "(id, layer_id) " +
                        " values " +
                        "('" + STYLE_PREVIEW_ID_PROTECTED_LAYER + "', '" + protectedLayerId + "')," +
                        "('" + STYLE_PREVIEW_ID_RESTRICTED_LAYER + "', '" + restrictedLayerId + "')"
        );
        runSql(
                "insert into t_style_preview_background_layer " +
                        "(id, style_preview_id, layer_id) " +
                        " values " +
                        "(23, '" + STYLE_PREVIEW_ID_PROTECTED_LAYER + "', '" + protectedLayer2Id + "')"
        );
        runSql(
                "insert into t_geoservice " +
                        "(id, name, stage, workspace) " +
                        " values " +
                        "(" + protectedLayerId + ", '" + protectedLayerWorkspaceAndName.get(1) + "', '" + Stage.CONFIGURATION.name() + "', '" + protectedLayerWorkspaceAndName.get(0) + "')," +
                        "(" + protectedLayer2Id + ", '" + protectedLayer2WorkspaceAndName.get(1) + "', '" + Stage.CONFIGURATION.name() + "', '" + protectedLayer2WorkspaceAndName.get(0) + "')," +
                        "(" + restrictedLayerId + ", '" + restrictedLayerWorkspaceAndName.get(1) + "', '" + Stage.CONFIGURATION.name() + "', '" + restrictedLayerWorkspaceAndName.get(0) + "')"
        );
    }

    @AfterEach
    void destroyStylePreviewDbState() {
        runSql(
                """
                drop table t_style_preview;
                drop table t_style_preview_background_layer;
                drop table t_geoservice;
                """
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessGrantedByStylePreview(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .mutateWith(
                        sessionMutator(sessionBuilder().put(
                                StylePreviewProperties.STYLE_PREVIEW_SESSION_ATTRIBUTE_NAME,
                                STYLE_PREVIEW_ID_RESTRICTED_LAYER
                        ).build()))
                .get()
                .uri(urlFragment + RESTRICTED_LAYER)
                .exchange();
        responseSpec.expectStatus().isOk();
        expectGeoServerResponse(responseSpec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessGrantedForBackgroundLayersByStylePreview(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .mutateWith(
                        sessionMutator(sessionBuilder().put(
                                StylePreviewProperties.STYLE_PREVIEW_SESSION_ATTRIBUTE_NAME,
                                STYLE_PREVIEW_ID_PROTECTED_LAYER
                        ).build()))
                .get()
                .uri(urlFragment + PROTECTED_LAYER2)
                .exchange();
        responseSpec.expectStatus().isOk();
        expectGeoServerResponse(responseSpec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessRestrictedWhenUsingWrongStylePreviewId(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .mutateWith(
                        sessionMutator(sessionBuilder().put(
                                StylePreviewProperties.STYLE_PREVIEW_SESSION_ATTRIBUTE_NAME,
                                STYLE_PREVIEW_ID_PROTECTED_LAYER
                        ).build()))
                .get()
                .uri(urlFragment + RESTRICTED_LAYER)
                .exchange();
        expectRedirectToKeyCloak(responseSpec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/geoserver/some_workspace/wms?SERVICE=WMS&layers=",
            "/geoserver/some_workspace/wfs?SERVICE=WFS&typenames=",
            "/geoserver/gwc/service/wmts?SERVICE=WMTS&layer=",
    })
    void accessRestrictedWhenUsingWrongStylePreviewIdAndNoSessionIsSet(String urlFragment) {
        mockGeoServer();
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri(urlFragment + RESTRICTED_LAYER)
                .exchange();
        expectRedirectToKeyCloak(responseSpec);
    }

    @Test
    void testRepo(){
        repository.findGeoServerLayersByStylePreviewId(STYLE_PREVIEW_ID_PROTECTED_LAYER)
                .as(StepVerifier::create)
                .expectNext(StylePreviewGeoServiceLayer.builder().stylePreviewId(STYLE_PREVIEW_ID_PROTECTED_LAYER).workspaceAndName(PROTECTED_LAYER).build())
                .expectNext(StylePreviewGeoServiceLayer.builder().stylePreviewId(STYLE_PREVIEW_ID_PROTECTED_LAYER).workspaceAndName(PROTECTED_LAYER2).build())
                .verifyComplete();

        repository.findGeoServerLayersByStylePreviewIdAndStage(STYLE_PREVIEW_ID_PROTECTED_LAYER, Stage.QS.name())
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }
}
