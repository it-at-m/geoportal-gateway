package de.swm.lhm.geoportal.gateway.style_preview;

import de.swm.lhm.geoportal.gateway.base_classes.config.TestSessionConfig;
import de.swm.lhm.geoportal.gateway.style_preview.repository.StylePreviewGeoServiceLayerRepository;
import de.swm.lhm.geoportal.gateway.style_preview.repository.StylePreviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.security.util.InMemoryResource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;


@ExtendWith(SpringExtension.class)
@ExtendWith({OutputCaptureExtension.class})
@Import({
        TestSessionConfig.class,
        StylePreviewProperties.class,
        ConfigStylePreviewService.class,
        StylePreviewGeoServiceLayerRepository.class,
        StylePreviewRepository.class
})
@Slf4j
class StylePreviewGeoServiceLayerServiceTest {

    @MockBean
    StylePreviewGeoServiceLayerRepository layerRepository;
    @MockBean
    StylePreviewRepository repository;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    ConfigStylePreviewService service;

    private static Stream<Arguments> generateNoIndexHtmlResources() {

        return Stream.of(
                Arguments.of(new FileSystemResource(new File("src/test/resources/de/swm/lhm/geoportal/gateway/style_preview/b8239f75-5f0c-41e4-ac4f-7f042aa3ef18/config.js"))),
                Arguments.of(new InMemoryResource("test"))
        );

    }

    private ServerWebExchange createExchange(WebSession session, URI url, String path) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        MockServerWebExchange.Builder builder = MockServerWebExchange.builder(request);
        if (session != null) {
            builder = builder.session(session);
        }
        MockServerWebExchange exchange = builder.build();
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, url);
        return exchange;
    }


    @Test
    void setAndGetSessionAttribute() throws URISyntaxException {

        URI url = new URI("http://test");
        WebSession session = new MockWebSession();
        ServerWebExchange exchange = createExchange(session, url, "/mypath");

        service.setSessionAttribute(exchange, "test")
                .as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();

        service.getSessionAttribute(exchange)
                .as(StepVerifier::create)
                .expectNext("test")
                .verifyComplete();

    }

    @Test
    void setSessionAttributeIfIndexHtmlTest() throws URISyntaxException {

        URI url = new URI("http://test");
        WebSession session = new MockWebSession();
        ServerWebExchange exchange = createExchange(session, url, "/mypath");

        Resource resource = new FileSystemResource(
                new File("src/test/resources/de/swm/lhm/geoportal/gateway/style_preview/b8239f75-5f0c-41e4-ac4f-7f042aa3ef18/index.html")
        );

        service.setSessionAttributeIfIndexHtml(resource, exchange, "test")
                .as(StepVerifier::create)
                .expectNext(resource)
                .verifyComplete();

        service.getSessionAttribute(exchange)
                .as(StepVerifier::create)
                .expectNext("test")
                .verifyComplete();

    }

    @ParameterizedTest
    @MethodSource("generateNoIndexHtmlResources")
    void setSessionAttributeIfIndexHtmlTestWithNoIndexHtml(Resource resource) throws URISyntaxException {

        URI url = new URI("http://test");
        WebSession session = new MockWebSession();
        ServerWebExchange exchange = createExchange(session, url, "/mypath");

        service.setSessionAttributeIfIndexHtml(resource, exchange, "test")
                .as(StepVerifier::create)
                .expectNext(resource)
                .verifyComplete();

        service.getSessionAttribute(exchange)
                .as(StepVerifier::create)
                .expectNextCount(0).
                verifyComplete();

    }

    @Test
    void isValidStylePreviewTest() {

        String id = UUID.randomUUID().toString();
        Mockito.when(repository.existsById(id)).thenReturn(Mono.just(true));

        service.isValidStylePreview(id)
                .as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();

    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "  ",
            "test",
    })
    void isValidStylePreviewTestFalse(String value) {

        service.isValidStylePreview(value)
                .as(StepVerifier::create)
                .expectNext(false)
                .verifyComplete();

    }

}