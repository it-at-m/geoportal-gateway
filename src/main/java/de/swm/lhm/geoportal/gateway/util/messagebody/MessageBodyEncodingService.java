package de.swm.lhm.geoportal.gateway.util.messagebody;

import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyDecoder;
import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles http body compression ("Content-Encoding" and "Accept-Encoding" headers)
 */
@Service
public class MessageBodyEncodingService {

    private final Map<String, MessageBodyDecoder> messageBodyDecoders;
    private final Map<String, MessageBodyEncoder> messageBodyEncoders;

    public MessageBodyEncodingService(Collection<MessageBodyDecoder> messageBodyDecoders, Collection<MessageBodyEncoder> messageBodyEncoders) {
        this.messageBodyDecoders = messageBodyDecoders
                .stream()
                .collect(Collectors.toMap(MessageBodyDecoder::encodingType, v -> v));
        this.messageBodyEncoders = messageBodyEncoders
                .stream()
                .collect(Collectors.toMap(MessageBodyEncoder::encodingType, v -> v));
    }


    public Optional<String> getContentEncoding(HttpHeaders headers) {
        return Optional.ofNullable(headers.getFirst(HttpHeaders.CONTENT_ENCODING))
                .filter(StringUtils::isNotBlank);
    }

    @SuppressWarnings("PMD")
    // from ModifyResponseBodyGatewayFilterFactory.java
    public byte[] decodeIncomingMessage(ServerHttpRequest request, byte[] message) {
        return getContentEncoding(request.getHeaders())
                .map(contentEncoding -> getDecoder(contentEncoding).decode(message))
                .orElse(message); // not encoded
    }

    private MessageBodyDecoder getDecoder(String encodingName) {
        MessageBodyDecoder decoder = this.messageBodyDecoders.get(encodingName);
        if (decoder == null) {
            throw new RuntimeException("No decoder available for encoding " + encodingName);
        }
        return decoder;
    }

    private MessageBodyEncoder getEncoder(String encodingName) {
        MessageBodyEncoder encoder = this.messageBodyEncoders.get(encodingName);
        if (encoder == null) {
            throw new RuntimeException("No decoder available for encoding " + encodingName);
        }
        return encoder;
    }

    public DataBuffer decodeDataBuffer(DataBuffer inputDataBuffer, String encodingName) {
        MessageBodyDecoder decoder = getDecoder(encodingName);
        byte[] inputBytes = DataBufferUtils.readDataBufferAsByteArray(inputDataBuffer);
        return inputDataBuffer.factory().wrap(decoder.decode(inputBytes));
    }

    public DataBuffer encodeDataBuffer(DataBuffer inputDataBuffer, String encodingName) {
        MessageBodyEncoder encoder = getEncoder(encodingName);
        return inputDataBuffer.factory().wrap(encoder.encode(inputDataBuffer));
    }

    public Set<String> getSupportedRequestContentEncodings() {
        return this.messageBodyDecoders.keySet();
    }

    public void removeUnsupportedAcceptEncodings(HttpHeaders headers) {
        List<String> acceptEncodings = headers.remove(HttpHeaders.ACCEPT_ENCODING);
        if (acceptEncodings == null || acceptEncodings.isEmpty()) {
            return;
        }

        String newEncodingsCsv = acceptEncodings.stream()
                .flatMap(encodingsCsv -> Arrays.stream(encodingsCsv.split(",")))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(encoding -> this.messageBodyEncoders.containsKey(encoding) && this.messageBodyDecoders.containsKey(encoding))
                .collect(Collectors.joining(", "));

        if (StringUtils.isNotBlank(newEncodingsCsv)) {
            headers.add(HttpHeaders.ACCEPT_ENCODING, newEncodingsCsv);
        }
    }
}
