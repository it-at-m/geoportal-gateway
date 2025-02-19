package de.swm.lhm.geoportal.gateway.print.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import de.swm.lhm.geoportal.gateway.geoservice.HostReplacer;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.HttpHeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JsonReplacePublicHostnamesGatewayFilter implements GatewayFilter {

    private final HostReplacer hostReplacer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isApplicable(request)) {
            ServerWebExchange mutatedExchange = exchange
                    .mutate()
                    .request(new ServerHttpRequestDecorator(request) {

                        @Override
                        public HttpHeaders getHeaders() {
                            HttpHeaders headers = HttpHeaders.writableHttpHeaders(super.getHeaders());

                            // remove content-length header as replacing hostnames may change the length and
                            // use chunked encoding instead as we can not access the reactive request body
                            // from this non-reactive method. Explicit blocking of TCP-related code is not allowed
                            // by reactor.
                            // Springs ModifyRequestBodyGatewayFilterFactory.java does the same.
                            headers.remove(HttpHeaders.CONTENT_LENGTH);
                            headers.set(HttpHeaders.TRANSFER_ENCODING, "chunked");

                            return headers;
                        }

                        @Override
                        public Flux<DataBuffer> getBody() {
                            return DataBufferUtils.copyAsByteArray(super.getBody())
                                    .map(jsonBytes -> dataBufferFactory.wrap(processJson(jsonBytes)))
                                    .flux();
                        }
                    })
                    .build();

            return chain.filter(mutatedExchange);
        } else {
            return chain.filter(exchange);
        }
    }

    protected boolean isApplicable(ServerHttpRequest request) {
        return HttpHeaderUtils.isContentTypeJson(request.getHeaders());
    }

    @SneakyThrows
    protected byte[] processJson(byte[] json) {
        String jsonText = new String(json, StandardCharsets.UTF_8);
        JsonNode jsonNode = replaceHostNames(objectMapper.readTree(jsonText));
        String outputJsonString = objectMapper.writeValueAsString(jsonNode);
        return outputJsonString.getBytes(StandardCharsets.UTF_8);
    }

    protected JsonNode replaceHostNames(JsonNode jsonNode) {
        return switch (jsonNode) {
            case ObjectNode objectNode -> {
                ObjectNode newObjectNode = objectMapper.createObjectNode();
                Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    newObjectNode.set(replaceHostNames(entry.getKey()), replaceHostNames(entry.getValue()));
                }
                yield newObjectNode;
            }
            case ArrayNode arrayNode -> {
                ArrayNode newArrayNode = objectMapper.createArrayNode();
                for (int i = 0; i < arrayNode.size(); i++) {
                    newArrayNode.add(replaceHostNames(arrayNode.get(i)));
                }
                yield newArrayNode;
            }
            case TextNode textNode -> new TextNode(replaceHostNames(textNode.textValue()));
            default -> jsonNode;
        };

    }

    private String replaceHostNames(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }
        String output = hostReplacer.replacePublicHostNames(input);
        if (log.isDebugEnabled() && !output.equals(input)) {
            log.debug("Replaced hostname in JSON: {} -> {}", input, output);
        }
        return output;
    }
}
