package de.swm.lhm.geoportal.gateway.util.messagebody;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DeflateMessageBodyResolverTest {

    @Test
    void testDecode() {
        DeflateMessageBodyResolver decoder = new DeflateMessageBodyResolver();
        String message = new String(decoder.decode(SampleMessages.MESSAGE1_DEFLATED_BYTES), StandardCharsets.UTF_8);
        assertThat(message, is(SampleMessages.MESSAGE1_CONTENT));
    }

    @Test
    void testEncode() {
        DeflateMessageBodyResolver encoder = new DeflateMessageBodyResolver();
        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(SampleMessages.MESSAGE1_CONTENT.getBytes(StandardCharsets.UTF_8));
        assertThat(encoder.encode(dataBuffer), is(SampleMessages.MESSAGE1_DEFLATED_BYTES));
    }
}
