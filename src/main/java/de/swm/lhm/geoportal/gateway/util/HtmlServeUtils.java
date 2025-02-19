package de.swm.lhm.geoportal.gateway.util;

import com.google.common.io.Resources;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Slf4j
@UtilityClass
public class HtmlServeUtils {

    public static Mono<Void> createHtmlResponse(ServerWebExchange exchange, String viewPath, HttpStatusCode statusCode) {

        if (exchange == null)
            return Mono.empty();

        ServerHttpResponse response = exchange.getResponse();

        String responseBody;
        try {
            responseBody = Resources.toString(Resources.getResource(viewPath), StandardCharsets.UTF_8);
        } catch (IOException | IllegalArgumentException e) {
            return Mono.error(e);
        }

        response.setStatusCode(statusCode);
        response.getHeaders().setContentType(MediaType.TEXT_HTML);
        DataBufferFactory bufferFactory = response.bufferFactory();
        DataBuffer buffer = bufferFactory.wrap(responseBody.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));

    }
}
