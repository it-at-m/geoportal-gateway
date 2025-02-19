package de.swm.lhm.geoportal.gateway.style_preview;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.session.Session;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static de.swm.lhm.geoportal.gateway.base_classes.config.TestSessionConfig.SESSION_MAP;
import static de.swm.lhm.geoportal.gateway.style_preview.StylePreviewProperties.STYLE_PREVIEW_SESSION_ATTRIBUTE_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class StylePreviewGeoServiceLayerControllerTest extends BaseIntegrationTest {

    @MockBean
    StylePreviewProperties stylePreviewProperties;

    private final static String EXISTING_STYLE_PREVIEW_ID = "b8239f75-5f0c-41e4-ac4f-7f042aa3ef18";

    @Value("${geoportal.gateway.style-preview.endpoint}")
    private String endpoint;

    @BeforeEach
    void setUp() {
        runSql(String.format("""
                CREATE TABLE IF NOT EXISTS t_style_preview
                (
                    id character varying(36) NOT NULL,
                    layer_type character varying(10),
                    layer_id character varying(255),
                    style_json text,
                    created_at timestamp without time zone DEFAULT now()
                );
                
                INSERT INTO t_style_preview
                (id)
                VALUES
                ('%s');
                """,
                EXISTING_STYLE_PREVIEW_ID
                ));

        SESSION_MAP.clear();

    }

    @AfterEach
    void tearDown() {
        runSql("DROP TABLE IF EXISTS t_style_preview");
    }

    @Test
    void redirectToIndexSuccess() {
        webTestClient.get()
                .uri( "/{style-preview-endpoint}/{style-preview-id}", endpoint.replace("/", ""), EXISTING_STYLE_PREVIEW_ID)
                .exchange()
                .expectStatus().isPermanentRedirect();
    }

    @ParameterizedTest
    @ValueSource(strings = {"eed06d75-6489-4975-99ea-9e21f60a32d3", "test", ""})
    void redirectToIndexFails(String stylePreviewId) {
        webTestClient.get()
                .uri("/{style-preview-endpoint}/{style-preview-id}", endpoint.replace("/", ""), stylePreviewId)
                .exchange()
                .expectStatus().isNotFound();

    }

    @Test
    void serveNonExistingIndex() {

        Mockito.when(stylePreviewProperties.getPath()).thenReturn("/dummy");

        webTestClient.get()
                .uri( "/{style-preview-endpoint}/{style-preview-id}/", endpoint.replace("/", ""), EXISTING_STYLE_PREVIEW_ID)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void serveIndex() throws IOException {

        Resource resource = new FileSystemResource(
                new File("src/test/resources/de/swm/lhm/geoportal/gateway/style_preview/b8239f75-5f0c-41e4-ac4f-7f042aa3ef18/index.html")
        );
        Mockito.when(stylePreviewProperties.getPath()).thenReturn(Path.of(resource.getURI()).getParent().getParent().toAbsolutePath().toString());

        webTestClient.get()
                .uri( "/{style-preview-endpoint}/{style-preview-id}/", endpoint.replace("/", ""), EXISTING_STYLE_PREVIEW_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody().xml("<html xmlns=\"http://www.w3.org/1999/xhtml\"></html>");

        assertThat(SESSION_MAP, aMapWithSize(1));
        Optional<String> sessionKey = SESSION_MAP.keySet().stream().findFirst();
        assertThat(sessionKey, isPresent());

        Session session = SESSION_MAP.get(sessionKey.get());
        Object attribute = session.getAttribute(STYLE_PREVIEW_SESSION_ATTRIBUTE_NAME);

        assertThat(attribute,  allOf(is(notNullValue()), instanceOf(String.class), is(EXISTING_STYLE_PREVIEW_ID)));

    }

    @ParameterizedTest
    @MethodSource("serveStylePreviewFileArguments")
    void serveStylePreviewFile(String relativeFilePath, String fileContent) throws IOException {

        Resource resource = new FileSystemResource(
                new File("src/test/resources/de/swm/lhm/geoportal/gateway/style_preview/b8239f75-5f0c-41e4-ac4f-7f042aa3ef18/config.json")
        );
        Mockito.when(stylePreviewProperties.getPath()).thenReturn(Path.of(resource.getURI()).getParent().getParent().toAbsolutePath().toString());
        Mockito.when(stylePreviewProperties.getEndpointWithSlashesLength()).thenReturn(15);

        webTestClient.get()
                .uri("/{style-preview-endpoint}/{style-preview-id}/" + relativeFilePath, endpoint.replace("/", ""), EXISTING_STYLE_PREVIEW_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody().json(fileContent);

        assertThat(SESSION_MAP, aMapWithSize(0));

    }

    @ParameterizedTest
    @MethodSource("serveStylePreviewFileArgumentsWithInvalidStylePreviewId")
    void serveStylePreviewFileNoExistingStylePreviewId(String relativeFilePath, String fileContent, String stylePreviewId) throws IOException {

        Resource resource = new FileSystemResource(
                new File("src/test/resources/de/swm/lhm/geoportal/gateway/style_preview/b8239f75-5f0c-41e4-ac4f-7f042aa3ef18/config.json")
        );
        Mockito.when(stylePreviewProperties.getPath()).thenReturn(Path.of(resource.getURI()).getParent().getParent().toAbsolutePath().toString());
        Mockito.when(stylePreviewProperties.getEndpointWithSlashesLength()).thenReturn(15);

        webTestClient.get()
                .uri("/{style-preview-endpoint}/{style-preview-id}/" + relativeFilePath, endpoint.replace("/", ""), stylePreviewId)
                .exchange()
                .expectStatus().isNotFound();

        assertThat(SESSION_MAP, aMapWithSize(0));

    }

    private static Stream<Arguments> serveStylePreviewFileArguments() {
        return Stream.of(
                arguments("config.json", "{}"),
                arguments("sub/sub.json", "{\"sub\": true}")

        );
    }

    private static Stream<Arguments> serveStylePreviewFileArgumentsWithInvalidStylePreviewId() {
        return Stream.of(
                arguments("config.json", "{}", "test"),
                arguments("sub/sub.json", "{\"sub\": true}", "test"),
                arguments("config.json", "{}", "eed06d75-6489-4975-99ea-9e21f60a32d3"),
                arguments("sub/sub.json", "{\"sub\": true}", "eed06d75-6489-4975-99ea-9e21f60a32d3"),
                arguments("config.json", "{}", ""),
                arguments("sub/sub.json", "{\"sub\": true}", "")
        );
    }
}
