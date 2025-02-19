package de.swm.lhm.geoportal.gateway.util.messagebody;

import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.factory.rewrite.GzipMessageBodyResolver;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


class MessageBodyEncodingServiceTest {
    private MessageBodyEncodingService emptyMessageBodyEncodingService;
    private MessageBodyEncodingService messageBodyEncodingService;

    @BeforeEach
    void setup() {
        emptyMessageBodyEncodingService = new MessageBodyEncodingService(Collections.emptyList(), Collections.emptyList());

        GzipMessageBodyResolver gzipResolver = new GzipMessageBodyResolver();
        DeflateMessageBodyResolver deflateResolver = new DeflateMessageBodyResolver();
        messageBodyEncodingService = new MessageBodyEncodingService(List.of(gzipResolver, deflateResolver), List.of(gzipResolver, deflateResolver));
    }

    @Test
    void getContentEncodingFromHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");

        assertThat(emptyMessageBodyEncodingService.getContentEncoding(headers))
                .isEqualTo(Optional.of("gzip"));
    }

    @Test
    void decodeDeflate() {
        DefaultDataBufferFactory dbf = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = dbf.wrap(SampleMessages.MESSAGE1_DEFLATED_BYTES);
        DataBuffer decoded = messageBodyEncodingService.decodeDataBuffer(dataBuffer, "deflate");
        assertThat(DataBufferUtils.readDataBufferAsString(decoded))
                .isEqualTo(SampleMessages.MESSAGE1_CONTENT);
    }

    @Test
    void decodeWithUnsupportedEncoding() {
        DefaultDataBufferFactory dbf = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = dbf.wrap(SampleMessages.MESSAGE1_DEFLATED_BYTES);
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> messageBodyEncodingService.decodeDataBuffer(dataBuffer, "doesnotexist"));
    }

    @Test
    void encodeDeflate() {
        DefaultDataBufferFactory dbf = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = dbf.wrap(SampleMessages.MESSAGE1_CONTENT.getBytes(StandardCharsets.UTF_8));
        DataBuffer encoded = messageBodyEncodingService.encodeDataBuffer(dataBuffer, "deflate");
        assertThat(DataBufferUtils.readDataBufferAsByteArray(encoded))
                .isEqualTo(SampleMessages.MESSAGE1_DEFLATED_BYTES);
    }

    @Test
    void encodeWithUnsupportedEncoding() {
        DefaultDataBufferFactory dbf = new DefaultDataBufferFactory();
        DataBuffer dataBuffer = dbf.wrap(SampleMessages.MESSAGE1_CONTENT.getBytes(StandardCharsets.UTF_8));
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> messageBodyEncodingService.encodeDataBuffer(dataBuffer, "doesnotexist"));
    }

    @Test
    void removeUnsupportedEncodings() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, zstd");

        messageBodyEncodingService.removeUnsupportedAcceptEncodings(headers);

        String headerContent = headers.getFirst(HttpHeaders.ACCEPT_ENCODING);
        assertThat(headerContent)
                .isNotBlank()
                .contains("gzip")
                .contains("deflate")
                .doesNotContain("zstd");
    }
}
