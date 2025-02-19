package de.swm.lhm.geoportal.gateway.filter.gatewayfilter;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.swm.lhm.geoportal.gateway.filter.response.BodyModifyingServerHttpResponse;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.HttpHeaderUtils;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
public class ReplaceStringInBodyGatewayFilter implements GatewayFilter, Ordered {
    private final MessageBodyEncodingService messageBodyEncodingService;
    String searchString;
    String replacementString;

    public ReplaceStringInBodyGatewayFilter(MessageBodyEncodingService messageBodyEncodingService, String searchString, String replacementString) {
        this.searchString = searchString;
        this.replacementString = replacementString;
        this.messageBodyEncodingService = messageBodyEncodingService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        BodyModifyingServerHttpResponse responseWrapper = new BodyModifyingServerHttpResponse(exchange.getResponse(), this.messageBodyEncodingService) {

            @Override
            public Mono<Boolean> supportsBodyRewrite() {
                return Mono.just(HttpHeaderUtils.isTextFormatContentType(getHeaders()));
            }

            @Override
            protected Mono<DataBuffer> processBody(Mono<DataBuffer> body) {
                return DataBufferUtils.copyAsString(body)
                        .map(bodyString -> getDelegate()
                                .bufferFactory()
                                .wrap(bodyString.replaceAll(searchString, replacementString).getBytes(UTF_8))
                        );
            }
        };

        return chain.filter(responseWrapper.mutateServerWebExchange(exchange));
    }


    @Override
    public int getOrder() {
        return FilterOrder.RESPONSE_BEFORE_NETTY_WRITE_FILTER.getOrder();
    }

}
