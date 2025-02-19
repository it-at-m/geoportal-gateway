package de.swm.lhm.geoportal.gateway.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Bytes;
import de.swm.lhm.geoportal.gateway.shared.exceptions.DeserializationException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

@Slf4j
@UtilityClass
public class DataBufferUtils {

    public sealed interface DataBufferContents<T> {

        T get();

        int getNumBytesContained();

        <R> DataBufferContents<R> mapContents(Function<T, R> converter);

        record Complete<T>(T contents, int numBytesContained) implements DataBufferContents<T> {

            @Override
            public T get() {
                return this.contents;
            }

            @Override
            public int getNumBytesContained() {
                return numBytesContained;
            }

            @Override
            public <R> DataBufferContents<R> mapContents(Function<T, R> converter) {
                return new DataBufferContents.Complete<>(
                        converter.apply(this.contents()),
                        this.numBytesContained()
                );
            }
        }

        record Partial<T>(T contents, int numBytesRead, int numBytesContained) implements DataBufferContents<T> {
            @Override
            public T get() {
                return this.contents;
            }


            @Override
            public int getNumBytesContained() {
                return numBytesContained;
            }

            @Override
            public <R> DataBufferContents<R> mapContents(Function<T, R> converter) {
                return new DataBufferContents.Partial<>(
                        converter.apply(this.contents()),
                        this.numBytesRead,
                        this.numBytesContained
                );
            }
        }
    }


    public static <T> Mono<T> copyAsObject(Flux<DataBuffer> body, Class<T> clazz) {
        return copyAsString(body).map(bodyString -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(bodyString, clazz);
            } catch (JsonProcessingException e) {
                throw new DeserializationException("The request could not be deseralized", e);
            }
        });
    }

    public static Mono<String> copyAsString(Flux<DataBuffer> body) {
        return copyAsByteArray(body)
                .map(String::new);
    }

    public static Mono<String> copyAsString(Mono<DataBuffer> body) {
        return body.map(DataBufferUtils::readDataBufferAsString);
    }

    public static Mono<DataBufferContents<String>> copyAsString(Flux<DataBuffer> body, int maxByteCount) {
        return copyAsByteArray(body, maxByteCount)
                .map(dataBufferContents -> dataBufferContents.mapContents(String::new));
    }

    public static Mono<byte[]> copyAsByteArray(Flux<DataBuffer> body) {
        return body.map(DataBufferUtils::readDataBufferAsByteArray)
                .collectList()
                .map(DataBufferUtils::joinByteArrayList);
    }

    public static Mono<byte[]> copyAsByteArray(Mono<DataBuffer> body) {
        return body.map(DataBufferUtils::readDataBufferAsByteArray);
    }

    public static Mono<DataBufferContents<byte[]>> copyAsByteArray(Flux<DataBuffer> body, int maxByteCount) {
        if (maxByteCount < 0) {
            throw new RuntimeException("maxByteCount must be positive");
        }

        Tuple3<Integer, Integer, List<byte[]>> accumulator = Tuples.of(0, 0, new ArrayList<>());

        // this consumes the complete stream, as an improvement it would be nice to
        // exit early when maxByteCount is reached
        return body
                .reduce(
                        accumulator,
                        (accum, dataBuffer) -> {
                            int numBytesRead = accum.getT1();
                            int numBytesContained = accum.getT2() + dataBuffer.readableByteCount();
                            List<byte[]> byteArrays = accum.getT3();
                            int numBytesLeftToRead = maxByteCount - numBytesRead;
                            if (numBytesLeftToRead > 0) {
                                byte[] byteArray = readDataBufferAsByteArray(dataBuffer, numBytesLeftToRead);
                                byteArrays.add(byteArray);
                                numBytesRead = numBytesRead + byteArray.length;
                            }
                            return Tuples.of(numBytesRead, numBytesContained, byteArrays);
                        }
                )
                .map(accum -> {
                    byte[] byteArray = joinByteArrayList(accum.getT3());
                    if (!Objects.equals(accum.getT1(), accum.getT2())) {
                        return new DataBufferContents.Partial<>(byteArray, accum.getT1(), accum.getT2());
                    } else {
                        return new DataBufferContents.Complete<>(byteArray, accum.getT1());

                    }
                });
    }

    public static byte[] readDataBufferAsByteArray(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length);
        dataBuffer.toByteBuffer(byteBuffer);
        return bytes;
    }

    public static String readDataBufferAsString(DataBuffer dataBuffer) {
        return new String(readDataBufferAsByteArray(dataBuffer), UTF_8);
    }

    private static byte[] readDataBufferAsByteArray(DataBuffer dataBuffer, int maxByteCount) {
        int numBytesToRead = Math.min(dataBuffer.readableByteCount(), maxByteCount);
        byte[] bytes = new byte[numBytesToRead];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 0, numBytesToRead);
        dataBuffer.toByteBuffer(0, byteBuffer, 0, numBytesToRead);
        return bytes;
    }

    public static byte[] joinByteArrayList(List<byte[]> byteArrayList) {
        List<Byte> result = new ArrayList<>();
        for (byte[] byteArray : byteArrayList) {
            result.addAll(Bytes.asList(byteArray));
        }
        return Bytes.toArray(result);
    }

    /**
     * helper function to release databuffers before they get garbage collected.
     */
    public static <T> Mono<T> withDataBufferRelease(DataBuffer dataBuffer, Function<DataBuffer, Mono<T>> func, Logger logger, Supplier<String> contextDescriptionSupplier) {
        return func.apply(dataBuffer)
                .doFinally(signalType -> releaseDataBuffer(dataBuffer, "doFinally", logger, contextDescriptionSupplier));
    }

    public static <T> Mono<T> withDataBufferRelease(DataBuffer dataBuffer, Function<DataBuffer, Mono<T>> func, Supplier<String> contextDescriptionSupplier) {
        return withDataBufferRelease(dataBuffer, func, log, contextDescriptionSupplier);
    }

    /**
     * Release a databuffer.
     *
     * This is less of a direct memory release, instead a reference count gets decremented and
     * the release only happens when there are no more references to the object in existence.
     */
    public static boolean releaseDataBuffer(DataBuffer dataBuffer, String releaseTriggerName, Logger logger, Supplier<String> contextDescriptionSupplier) {
        boolean wasReleased = org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
        if (logger.isTraceEnabled()) {
            if (wasReleased) {
                logger.trace(String.format(
                        "Released a dataBuffer containing %d bytes (trigger: %s, context: %s)",
                        dataBuffer.readableByteCount(),
                        releaseTriggerName,
                        contextDescriptionSupplier.get()
                ));
            } else {
                logger.trace(String.format(
                        "Unsuccessfully attempted to release a dataBuffer containing %d bytes (trigger: %s, context: %s)",
                        dataBuffer.readableByteCount(),
                        releaseTriggerName,
                        contextDescriptionSupplier.get()
                ));
            }
        }
        return wasReleased;
    }

    public static boolean releaseDataBuffer(DataBuffer dataBuffer, Logger logger, Supplier<String> contextDescriptionSupplier) {
        return releaseDataBuffer(dataBuffer, "unknown", logger, contextDescriptionSupplier);
    }
}
